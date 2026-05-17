#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN_USER=""
TOKEN_ADMIN=""
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@raktakk.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@12345}"
RUN_TOKEN_SECURITY_CHECK="${RUN_TOKEN_SECURITY_CHECK:-1}"

TMP_DIR="$(mktemp -d)"
LAST_BODY_FILE=""
trap 'rm -rf "$TMP_DIR"' EXIT

fail() {
  echo "❌ Error: $1" >&2
  if [[ -n "$LAST_BODY_FILE" && -f "$LAST_BODY_FILE" ]]; then
    echo "--- Response body ---" >&2
    cat "$LAST_BODY_FILE" >&2
    echo >&2
    echo "---------------------" >&2
  fi
  exit 1
}

log_step() {
  echo
  echo "✅ $1"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing prerequisite: $1"
}

json_get() {
  local file="$1"
  local path="$2"

  if command -v jq >/dev/null 2>&1; then
    jq -r "$path" "$file"
    return
  fi

  if command -v python3 >/dev/null 2>&1; then
    python3 - "$file" "$path" <<'PY'
import json, sys
file_path = sys.argv[1]
path = sys.argv[2].lstrip('.')
with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)
node = data
if path:
    for key in path.split('.'):
        if isinstance(node, dict) and key in node:
            node = node[key]
        else:
            print("")
            sys.exit(0)
if isinstance(node, bool):
    print("true" if node else "false")
elif node is None:
    print("")
else:
    print(node)
PY
    return
  fi

  if command -v node >/dev/null 2>&1; then
    node -e "const fs=require('fs');const data=JSON.parse(fs.readFileSync(process.argv[1],'utf8'));const p=process.argv[2].replace(/^\./,'').split('.').filter(Boolean);let n=data;for(const k of p){if(n&&typeof n==='object'&&k in n){n=n[k]}else{n='';break}};if(typeof n==='boolean') process.stdout.write(n?'true':'false'); else process.stdout.write(n==null?'':String(n));" "$file" "$path"
    return
  fi

  fail "No JSON parser available. Install jq (recommended), python3, or node."
}

vendors_all_verified() {
  local file="$1"

  if command -v jq >/dev/null 2>&1; then
    jq -e 'all(.[]; .vendorVerified == true)' "$file" >/dev/null
    return
  fi

  if command -v python3 >/dev/null 2>&1; then
    python3 - "$file" <<'PY'
import json, sys
with open(sys.argv[1], 'r', encoding='utf-8') as f:
    arr = json.load(f)
ok = all(item.get('vendorVerified') is True for item in arr)
sys.exit(0 if ok else 1)
PY
    return
  fi

  if command -v node >/dev/null 2>&1; then
    node -e "const fs=require('fs');const arr=JSON.parse(fs.readFileSync(process.argv[1],'utf8'));process.exit(arr.every(v=>v.vendorVerified===true)?0:1);" "$file"
    return
  fi

  fail "No JSON parser available to validate vendors."
}

request() {
  local name="$1"
  local method="$2"
  local url="$3"
  local auth_token="${4:-}"
  local payload="${5:-}"

  local body_file="$TMP_DIR/${name}.json"
  local headers=(-H "Content-Type: application/json")

  if [[ -n "$auth_token" ]]; then
    headers+=(-H "Authorization: Bearer $auth_token")
  fi

  local status
  if [[ -n "$payload" ]]; then
    status=$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" "$url" "${headers[@]}" -d "$payload")
  else
    status=$(curl -sS -o "$body_file" -w "%{http_code}" -X "$method" "$url" "${headers[@]}")
  fi

  LAST_BODY_FILE="$body_file"
  echo "$status"
}

assert_status_in() {
  local actual="$1"
  shift
  local expected=($*)
  for code in "${expected[@]}"; do
    if [[ "$actual" == "$code" ]]; then
      return
    fi
  done
  fail "Unexpected HTTP status: $actual (expected one of: ${expected[*]})"
}

assert_equals() {
  local actual="$1"
  local expected="$2"
  local label="$3"
  if [[ "$actual" != "$expected" ]]; then
    fail "$label expected '$expected' but got '$actual'"
  fi
}

assert_non_empty() {
  local value="$1"
  local label="$2"
  if [[ -z "$value" || "$value" == "null" ]]; then
    fail "$label must not be empty"
  fi
}

require_cmd curl

echo "🚀 Running marketplace E2E against: $BASE_URL"
if ! command -v jq >/dev/null 2>&1; then
  echo "ℹ️ jq not found: using fallback JSON parser (python3/node)."
fi

PING_STATUS=$(request "ping_vendors" "GET" "$BASE_URL/api/vendors")
assert_status_in "$PING_STATUS" 200

NOW="$(date +%s)"
EMAIL="e2e_marketplace_${NOW}@raktakk.com"
PASSWORD="User@12345"
FULL_NAME="E2E Marketplace User"

log_step "Step 1: Register user"
REG_STATUS=$(request "register" "POST" "$BASE_URL/api/auth/register" "" "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"fullName\":\"$FULL_NAME\"}")
assert_status_in "$REG_STATUS" 200 201
TOKEN_USER=$(json_get "$TMP_DIR/register.json" '.token')
USER_ID=$(json_get "$TMP_DIR/register.json" '.user.id')
USER_ROLE=$(json_get "$TMP_DIR/register.json" '.user.role')
assert_non_empty "$TOKEN_USER" "TOKEN_USER"
assert_non_empty "$USER_ID" "USER_ID"
assert_equals "$USER_ROLE" "USER" "Registered user role"

USER_STATUS=$(request "vendor_status_before" "GET" "$BASE_URL/api/vendor/status" "$TOKEN_USER")
assert_status_in "$USER_STATUS" 200
IS_VENDOR_BEFORE=$(json_get "$TMP_DIR/vendor_status_before.json" '.isVendor')
IS_VERIFIED_BEFORE=$(json_get "$TMP_DIR/vendor_status_before.json" '.vendorVerified')
assert_equals "$IS_VENDOR_BEFORE" "false" "profile.isVendor before become"
assert_equals "$IS_VERIFIED_BEFORE" "false" "profile.vendorVerified before become"

log_step "Step 2: Public consultation"
SERVICES_STATUS=$(request "services_public_before" "GET" "$BASE_URL/api/services")
VENDORS_STATUS=$(request "vendors_public_before" "GET" "$BASE_URL/api/vendors")
assert_status_in "$SERVICES_STATUS" 200
assert_status_in "$VENDORS_STATUS" 200
if ! vendors_all_verified "$TMP_DIR/vendors_public_before.json"; then
  fail "Public vendors list contains unverified vendors"
fi

log_step "Step 3: Become vendor"
BECOME_STATUS=$(request "become_vendor" "POST" "$BASE_URL/api/vendor/become" "$TOKEN_USER" '{"bio":"E2E marketplace vendor profile","phone":"771234567","avatar":"https://example.com/avatar-e2e.png"}')
assert_status_in "$BECOME_STATUS" 200
IS_VENDOR_AFTER=$(json_get "$TMP_DIR/become_vendor.json" '.isVendor')
assert_equals "$IS_VENDOR_AFTER" "true" "profile.isVendor after become"

STATUS_AFTER_BECOME=$(request "vendor_status_after" "GET" "$BASE_URL/api/vendor/status" "$TOKEN_USER")
assert_status_in "$STATUS_AFTER_BECOME" 200
IS_VERIFIED_AFTER=$(json_get "$TMP_DIR/vendor_status_after.json" '.vendorVerified')
assert_equals "$IS_VERIFIED_AFTER" "false" "profile.vendorVerified after become"

log_step "Step 4: Refuse service creation before admin verification"
CREATE_DENIED_STATUS=$(request "create_service_denied" "POST" "$BASE_URL/api/services" "$TOKEN_USER" '{"title":"Service should fail","description":"This call must be forbidden before verification","price":15000,"category":"QA","deliveryTime":3,"featured":false}')
assert_status_in "$CREATE_DENIED_STATUS" 403

log_step "Step 5: Admin validates vendor"
ADMIN_LOGIN_STATUS=$(request "admin_login" "POST" "$BASE_URL/api/auth/login" "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}")
assert_status_in "$ADMIN_LOGIN_STATUS" 200
TOKEN_ADMIN=$(json_get "$TMP_DIR/admin_login.json" '.token')
assert_non_empty "$TOKEN_ADMIN" "TOKEN_ADMIN"

VERIFY_STATUS=$(request "vendor_verify" "POST" "$BASE_URL/api/vendor/verify" "$TOKEN_ADMIN" "{\"userId\":$USER_ID}")
assert_status_in "$VERIFY_STATUS" 200
IS_VERIFIED_FINAL=$(json_get "$TMP_DIR/vendor_verify.json" '.vendorVerified')
assert_equals "$IS_VERIFIED_FINAL" "true" "profile.vendorVerified after admin verify"

log_step "Step 6: Create service as verified vendor"
SERVICE_TITLE="Service E2E ${NOW}"
CREATE_OK_STATUS=$(request "create_service_ok" "POST" "$BASE_URL/api/services" "$TOKEN_USER" "{\"title\":\"$SERVICE_TITLE\",\"description\":\"Service created by automated E2E script\",\"price\":25000,\"category\":\"Tech\",\"deliveryTime\":5,\"featured\":true}")
assert_status_in "$CREATE_OK_STATUS" 200 201
SERVICE_ID=$(json_get "$TMP_DIR/create_service_ok.json" '.id')
assert_non_empty "$SERVICE_ID" "SERVICE_ID"

log_step "Step 7: Validate public visibility"
SERVICES_AFTER_STATUS=$(request "services_public_after" "GET" "$BASE_URL/api/services")
assert_status_in "$SERVICES_AFTER_STATUS" 200
if command -v jq >/dev/null 2>&1; then
  jq -e --argjson serviceId "$SERVICE_ID" 'any(.[]; .id == $serviceId)' "$TMP_DIR/services_public_after.json" >/dev/null || fail "Created service not found in public services"
elif command -v python3 >/dev/null 2>&1; then
  python3 - "$TMP_DIR/services_public_after.json" "$SERVICE_ID" <<'PY'
import json, sys
with open(sys.argv[1], 'r', encoding='utf-8') as f:
    arr = json.load(f)
sid = int(sys.argv[2])
ok = any(item.get('id') == sid for item in arr)
sys.exit(0 if ok else 1)
PY
  [[ $? -eq 0 ]] || fail "Created service not found in public services"
else
  node -e "const fs=require('fs');const arr=JSON.parse(fs.readFileSync(process.argv[1],'utf8'));const id=Number(process.argv[2]);process.exit(arr.some(s=>s.id===id)?0:1);" "$TMP_DIR/services_public_after.json" "$SERVICE_ID" || fail "Created service not found in public services"
fi

if [[ "$RUN_TOKEN_SECURITY_CHECK" == "1" ]]; then
  log_step "Bonus: Security checks (401/403)"
  NO_AUTH_STATUS=$(request "users_me_no_auth" "GET" "$BASE_URL/api/users/me")
  assert_status_in "$NO_AUTH_STATUS" 401

  FORBIDDEN_STATUS=$(request "verify_with_user" "POST" "$BASE_URL/api/vendor/verify" "$TOKEN_USER" "{\"userId\":$USER_ID}")
  assert_status_in "$FORBIDDEN_STATUS" 403
fi

echo
echo "🎉 Marketplace E2E successful"
echo "BASE_URL=$BASE_URL"
echo "Test user email: $EMAIL"
echo "Created userId: $USER_ID"
echo "Created serviceId: $SERVICE_ID"
echo "ℹ️ Cleanup is not automatic because delete endpoints are not exposed for this flow."

BASE_URL ?= http://localhost:8080
FRONTEND_DIR ?= ../raktakk-react-frontend

.PHONY: e2e-marketplace test-backend build-frontend check

e2e-marketplace:
	@echo "🚀 Running E2E marketplace tests on $(BASE_URL)..."
	BASE_URL=$(BASE_URL) ./scripts/e2e-marketplace.sh

test-backend:
	@echo "🧪 Running backend tests..."
	mvn test

build-frontend:
	@echo "🏗️ Building frontend..."
	npm --prefix $(FRONTEND_DIR) run build

check:
	@echo "🔍 Running full validation..."
	$(MAKE) test-backend
	$(MAKE) build-frontend
	$(MAKE) e2e-marketplace

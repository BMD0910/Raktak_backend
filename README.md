# Raktakk Spring Backend

Backend Spring Boot sécurisé (JWT + OAuth2 Google) prêt pour Render + PostgreSQL.

## Stack
- Spring Boot 3
- Spring Security (JWT + OAuth2 Client)
- Spring Data JPA
- PostgreSQL (local + production Render)

## Architecture
- `controller/`
- `service/`
- `repository/`
- `entity/`
- `security/`
- `dto/`
- `exception/`

## Endpoints principaux
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/users/me`
- `GET /api/users/admin?page=0&size=10` (ADMIN)
- `GET /api/public/health`
- `GET /health`
- `GET /api/public/categories`
- `GET /api/public/subcategories?categoryId=1`
- `GET /oauth2/authorization/google`

## Compatibilité migration backend
- Les routes historiques utilisées par le frontend restent exposées par Spring Boot pour compatibilité, notamment `GET /api/admin/settings.php`, `POST /api/admin/settings-save.php`, `GET /api/admin/services.php`, `GET /api/admin/requests.php`, `GET /api/public/settings`, `GET /api/public/vendors` et `GET /api/public/subscription-plans`.
- Le dossier PHP `raktakk-hostinger-backend` a été retiré ; la configuration fonctionnelle vit désormais dans ce projet Spring Boot.

## Lancer en local
```bash
mvn spring-boot:run
```
- Profil actif par défaut : `dev`
- Base de données : PostgreSQL via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

## Installation PostgreSQL local

### Ubuntu
```bash
sudo apt install postgresql postgresql-contrib
```

### Création de la base
```sql
CREATE DATABASE raktakk_db;
```

### Variables d'environnement
```bash
export DB_URL=jdbc:postgresql://localhost:5432/raktakk_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

### Run
```bash
mvn spring-boot:run
```

## Build
```bash
mvn -DskipTests package
```

## Script E2E Marketplace
Prérequis:
- Backend Spring Boot lancé
- Base de données prête
- `curl` disponible (`jq` recommandé, fallback python3/node intégré)

Commande:
```bash
chmod +x scripts/e2e-marketplace.sh
./scripts/e2e-marketplace.sh
```

Variables utiles:
```bash
BASE_URL=http://localhost:8080
ADMIN_EMAIL=admin@raktakk.com
ADMIN_PASSWORD=Admin@12345
RUN_TOKEN_SECURITY_CHECK=1
```

Exemple:
```bash
BASE_URL=http://127.0.0.1:8085 ./scripts/e2e-marketplace.sh
```

## Variables d'environnement
Copier `.env.example` et renseigner les variables d'environnement nécessaires.

## Stratégie JWT (production)
- Access token court (`JWT_ACCESS_EXPIRATION_MS`, défaut 15 min)
- Refresh token long (`JWT_REFRESH_EXPIRATION_MS`, défaut 14 jours)
- Refresh token stocké en cookie `HttpOnly` + `Secure` + `SameSite`
- Rotation refresh token à chaque `/api/auth/refresh`
- Révocation refresh token au logout

## Protection brute-force
- Rate limiting actif sur:
  - `POST /api/auth/login`
  - `POST /api/auth/register`
- Limite par IP: 5 tentatives/minute

## Headers sécurité
- Content-Security-Policy
- X-Frame-Options
- X-Content-Type-Options
- HTTPS forcé en prod via `REQUIRE_HTTPS=true`

## CORS (front-end → backend)
- Définir `CORS_ALLOWED_ORIGINS` avec le domaine frontend exact.
- Exemple: `https://raktakk-front.netlify.app`
- Plusieurs domaines possibles séparés par virgule.

## Déploiement Render + PostgreSQL
1. Créer un service web Render à partir de `render.yaml`.
2. Attacher la base PostgreSQL Render.
3. Définir les variables d'environnement listées plus bas.
4. Déployer et tester `GET /health`.

Le backend démarre avec les variables Spring standards `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` et `SPRING_DATASOURCE_PASSWORD`. Si vous utilisez la base PostgreSQL Render du blueprint, l'application sait aussi convertir `DATABASE_URL` en JDBC au démarrage.

## OAuth2 Google
- Déclarer l'URI de callback côté Google :
  - `https://<backend-domain>/login/oauth2/code/google`
- Définir `GOOGLE_CLIENT_ID` et `GOOGLE_CLIENT_SECRET`.
- Après login, backend redirige vers `OAUTH2_SUCCESS_REDIRECT_URI?token=...`.

## Render

Variables à définir sur le service web Render:
- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `JWT_REFRESH_SECRET`
- `UNITECH_API_KEY`
- `CORS_ALLOWED_ORIGINS`
- `OAUTH2_SUCCESS_REDIRECT_URI`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`
- `MAIL_ADMIN`

Variables PostgreSQL que Render injecte si vous utilisez une base Render:
- `DATABASE_URL`
- `PGHOST`
- `PGPORT`
- `PGDATABASE`
- `PGUSER`
- `PGPASSWORD`
# Raktak_backend
# Raktak_backend

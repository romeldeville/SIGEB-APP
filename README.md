# SIGEB — Système Informatique de Gestion des Cartes de Baptême

Plateforme nationale centralisée de gestion des registres et cartes de baptême de l'**Église Catholique du Cameroun** (CENC → Provinces Ecclésiastiques → Diocèses / Archidiocèses → Paroisses / Cathédrales / Missions).

## Architecture du Monorepo

```text
SIGEB-APP/
├── .github/workflows/       # Pipelines CI GitHub Actions (backend.yml, frontend.yml)
├── backend/                 # API Spring Boot (Java 25, PostgreSQL 16, Flyway, OpenPDF + ZXing)
├── frontend/                # Application unifiée Flutter (Android, iOS, Web · Riverpod + GoRouter + Dio)
├── infra/                   # Docker Compose (dev & prod), Caddyfile (HTTPS auto), scripts backup PostgreSQL
├── docs/
│   ├── conception/          # Cadrage, Cas d'utilisation, MCD (14 tables), Architecture technique
│   └── presentations/       # Supports Marp (.md / .pdf) & diagrammes (assets/)
├── .env.example             # Modèle des variables d'environnement (aucun secret dans Git)
└── README.md
```

## Principes Clés
- **100 % Base de Données (PostgreSQL 16)** : aucun gestionnaire de fichiers externe ; les instantanés de cartes/duplicatas sont persistés en JSONB et les PDF/QR sont générés à la volée.
- **Sécurité & Droit Canonique** : séparation `NiveauEcclesial` (`NATIONAL`, `PROVINCE`, `DIOCESE`, `PAROISSE`) + `Role` (`ADMIN_NATIONAL`, `RESPONSABLE_PROVINCE`, `CHANCELIER`, `CURE`, `SECRETAIRE`), Row-Level Security PostgreSQL, Argon2id (MDP & PIN), JWT court + Refresh Token rotatif anti-rejeu, 2FA TOTP, clés QR versionnées (`SIGEB:v2:{kid}:{token}:{sig}`), journal d'audit chaîné SHA-256 `append-only`.
- **Bilinguisme natif** : Français / Anglais (Cameroun).

## Démarrage Rapide (Développement)

```powershell
# 1. PostgreSQL 16 local
docker compose -f infra/docker-compose.dev.yml up -d

# 2. Backend Spring Boot (Swagger UI : http://localhost:8080/swagger-ui.html)
mvn -f backend/pom.xml spring-boot:run

# 3. Frontend Flutter Web (Chrome)
cd frontend
flutter run -d chrome
```

## Tests
```powershell
# Backend (Flyway + Testcontainers PostgreSQL 16)
mvn -f backend/pom.xml test

# Frontend (analyse statique + tests widget)
cd frontend
dart analyze
flutter test
```

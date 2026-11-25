# Architecture Technique & Organisation Projet — SIGEB (v2)

> **Approche** : **Monolithe Modulaire** Spring Boot + **Frontend Unifié** Flutter (Mobile & Web) + **PostgreSQL 16** (100 % BD, sans gestionnaire de fichiers).  
> **Objectif** : Fiabilité institutionnelle sur 10+ ans, simplicité d'exploitation pour 1 développeur, sécurité de niveau bancaire, déploiement léger sur VPS.  
> **Version 2** : intègre la revue d'architecture (clés QR versionnées, RLS, audit infalsifiable, PITR, idempotence, CI/CD, conformité).

---

## 1. Décisions d'Architecture Fondamentales

| Sujet | Choix retenu | Pourquoi |
| :--- | :--- | :--- |
| **Style Backend** | **Monolithe Modulaire** *(Spring Boot 3.4+, Java 21, Maven)* + **Spring Modulith / ArchUnit** | Transactions ACID natives (scellement IUN, duplicatas), maintenable seul. Les tests de frontières empêchent la dérive vers un monolithe « spaghetti ». |
| **Base de données** | **PostgreSQL 16** + **Flyway** (`ddl-auto=validate`) | Intégrité stricte, JSONB pour instantanés de cartes et avenants, migrations versionnées. |
| **Zéro gestionnaire de fichiers** | Rendu PDF **à la volée** *(OpenPDF + ZXing)* depuis les données + `template_version` | Aucune dépendance S3/MinIO. Le `template_version` stocké dans `carte_emise` garantit qu'un duplicata régénéré dans 5 ans reste fidèle à la carte d'origine. |
| **Authentification** | **JWT court (15 min)** + **Refresh Token rotatif** (stocké **haché** en BD) | Révocation instantanée ; périmètre (`dioceseId`, `paroisseId`) dérivé du token, jamais du client. |
| **Comptes sensibles** | **PIN/MDP Argon2id**, verrouillage progressif après échecs, **2FA (TOTP)** obligatoire pour Curé, Chancelier et Admins | Ces rôles peuvent sceller/rectifier des actes : cibles prioritaires d'attaque. |
| **Cloisonnement** | **Double barrière** : filtre applicatif (`TenantContext`) **+ Row-Level Security PostgreSQL** | Un oubli de filtre dans le code n'expose plus d'autres paroisses : la base refuse. |
| **Signature QR** | **HMAC-SHA256**, clés **versionnées** (`kid`), format `SIGEB:v2:{kid}:{token}:{sig}` | Rotation de clé sans invalider les cartes imprimées. Token opaque ≥ 128 bits, aucune donnée personnelle dans le QR, révocation instantanée au duplicata. |
| **Frontend Unifié** | **Flutter** (Android/iOS + Web) · **Riverpod** · **GoRouter** · **Dio** + client Dart généré depuis OpenAPI | Codebase unique, RBAC sur les routes, i18n FR/EN, pas de duplication manuelle des DTO. |
| **Déploiement** | **Docker Compose** + **Caddy** *(HTTPS auto)*, environnements **dev / staging / prod** | Simple et reproductible. Le staging sert à valider migrations et pilote avant la prod. |

---

## 2. Organisation du Monorepo (`e:\SIGEB-APP`)

```text
SIGEB-APP/
├── .github/workflows/             # CI : backend.yml, frontend.yml, release.yml
├── docs/                          # Cadrage, Cas d'utilisation, MCD, Présentations Marp & assets/
├── infra/
│   ├── docker-compose.dev.yml     # PostgreSQL 16 local
│   ├── docker-compose.staging.yml # Pré-production (pilote, tests de migration)
│   ├── docker-compose.prod.yml    # Caddy + Spring Boot + PostgreSQL 16
│   ├── Caddyfile                  # /api -> backend, / -> Flutter Web, rate limiting
│   └── scripts/
│       ├── backup_pg.sh           # pg_dump chiffré + envoi hors-serveur
│       └── restore_test.sh        # Restauration automatisée mensuelle (preuve de sauvegarde)
├── backend/                       # Projet Maven Spring Boot (Java 21)
└── frontend/                      # Projet Flutter (Android, iOS, Web)
```

---

## 3. Architecture Backend (`backend/` — Package by Feature)

```text
backend/src/main/java/cm/cenc/sigeb/
├── SigebApplication.java
├── shared/                        # Socle transverse (sans logique métier)
│   ├── security/                  # JwtService, TenantContext (+ SET LOCAL RLS), RbacGuards, RateLimit
│   ├── exception/                 # GlobalExceptionHandler (RFC 7807 ProblemDetail FR/EN)
│   ├── i18n/                      # messages_fr / messages_en
│   ├── idempotence/               # Filtre Idempotency-Key (clé + hash requête + réponse)
│   └── crypto/                    # Argon2id (PIN/MDP), SHA-256 canonique, HMAC QR, KeyRing (kid)
├── auth/                          # UC-01 : Login, 2FA TOTP, Refresh rotatif, verrouillage, révocation
├── audit/                         # UC-14 : Journal append-only chaîné (mis en place dès S2)
├── referentiel/                   # UC-02..04 : Provinces, Diocèses, Paroisses, Utilisateurs, Affectations
├── bapteme/                       # UC-05..08 : Actes, anti-doublon, compteur IUN, scellement, avenants
├── mention/                       # UC-12..13 : Mentions marginales inter-paroisses (B -> A)
├── carte/                         # UC-09 : carte_emise (originale/duplicata), révocation, PDF/QR à la volée
├── verification/                  # UC-10..11 : Scan QR public limité + recherche nationale tracée
└── statistique/                   # UC-15 : Agrégations SQL natives + export CSV/PDF
```

### Règles obligatoires côté Backend
- **DTOs en `record`** : aucune entité JPA exposée dans un contrôleur.
- **Cloisonnement en profondeur** : filtre `TenantContext` **et** politiques RLS (`USING (paroisse_id = current_setting('app.paroisse_id')::bigint …)`), le contexte étant posé par `SET LOCAL` à chaque transaction.
- **Compteur IUN** : table `compteur_iun` à granularité **(paroisse, année)** ; incrément atomique par `SELECT … FOR UPDATE` sur cette seule ligne → aucune contention nationale, aucun trou de numérotation.
- **Concurrence & réseau instable** :
  - `Idempotency-Key` obligatoire sur les créations (acte, avenant, carte, mention) → un double envoi ne crée jamais de doublon.
  - Colonne `version` (verrouillage optimiste) sur `acte_bapteme` et `avenant`.
- **Audit infalsifiable** :
  - Événements Spring (`@TransactionalEventListener`) vers `journal_audit`.
  - `REVOKE UPDATE, DELETE, TRUNCATE` sur `journal_audit` pour le rôle applicatif.
  - Chaînage : chaque ligne contient `hash_precedent` et `hash_ligne` (SHA-256) ; un job de vérification d'intégrité tourne chaque nuit.
- **Gestion des clés QR** :
  - Table `cle_signature(kid, secret_chiffre, statut{ACTIVE,VERIFY_ONLY,RETIRED}, cree_le)`.
  - Signature avec la clé `ACTIVE` ; vérification avec toute clé `ACTIVE`/`VERIFY_ONLY`. Secret maître de chiffrement hors base (variable d'environnement ou coffre).
- **Point de vérification QR public** (`POST /api/v1/qr/verify`) :
  - **Rate limiting** par IP (Caddy + Bucket4j) et délai constant des réponses.
  - Scan anonyme → réponse **minimale** (`valide`, `statut`, nom abrégé). Détail complet (état libre, mentions) **réservé à un utilisateur authentifié et autorisé**.
  - Chaque scan est tracé (token, IP hachée, horodatage).
- **Migrations Flyway** :
  - `V1__init_schema.sql` : tables du MCD + contraintes + index + politiques RLS + droits restreints sur l'audit.
  - `V2__seed_cameroun.sql` : 5 provinces ecclésiastiques + Super Admin initial (changement de mot de passe et activation 2FA forcés à la 1re connexion).
- **Qualité** :
  - OpenAPI 3 (`/swagger-ui.html`), contrat versionné `/api/v1`.
  - Tests d'intégration **Testcontainers** : scellement IUN concurrent, duplicata/révocation, scan QR, RBAC **et RLS**, idempotence.
  - Tests ArchUnit/Modulith : frontières entre modules vérifiées à chaque build.

---

## 4. Architecture Frontend (`frontend/` — Feature-First Flutter)

```text
frontend/lib/
├── main.dart
├── app/
│   ├── router.dart                # GoRouter + redirection selon rôle RBAC
│   ├── theme.dart                 # Charte (Bleu nuit #1B2A4A & Or #C59B27)
│   └── l10n/                      # app_fr.arb & app_en.arb
├── core/
│   ├── api/                       # Client Dart généré depuis openapi.yaml (ne pas éditer)
│   ├── network/                   # Dio + AuthInterceptor (refresh auto, Accept-Language, Idempotency-Key)
│   ├── storage/                   # Mobile : flutter_secure_storage ; Web : voir ci-dessous
│   ├── drafts/                    # Brouillons locaux de saisie (tolérance aux coupures réseau)
│   └── widgets/                   # AdaptiveScaffold, badges statut
└── features/
    ├── auth/                      # Connexion, 2FA, changement MDP/PIN, sélecteur FR/EN
    ├── dashboard/                 # Tableau de bord par rôle
    ├── referentiel/               # Provinces, Diocèses, Paroisses, Comptes & Mutations
    ├── baptemes/                  # Liste, formulaire canonique, file « À valider », avenants
    ├── cartes/                    # Émission originale & duplicata, aperçu/impression (`printing`)
    ├── verification/              # Scanner QR (`mobile_scanner`), résultat, recherche nationale
    ├── mentions/                  # Déclaration sortante (B) & boîte de validation (A)
    └── audit_stats/               # Journal filtrable & statistiques
```

### Règles d'or côté Flutter
- **Un seul écran adaptatif Mobile + Web** (`AdaptiveScaffold`).
- **Stockage des jetons** :
  - Mobile : `flutter_secure_storage`.
  - **Web : refresh token en cookie `HttpOnly; Secure; SameSite=Strict`, access token uniquement en mémoire** (jamais `localStorage`) → résistance au vol par XSS.
- **Impression sans gestionnaire de fichiers** : le backend renvoie les octets PDF, `printing` ouvre l'impression/partage natif.
- **Saisie fiable** : validation client, erreurs RFC 7807 localisées, **brouillon local** conservé en cas de coupure, renvoi avec la même `Idempotency-Key`.
- **Mode hors-ligne complet** : **hors MVP** (décision actée). Seul le brouillon local est prévu ; la synchronisation différée reste une option de Phase 2.

---

## 5. Sécurité, Exploitation & Sauvegardes (Production)

```text
[Flutter Android/iOS & Navigateur Web]
                 │ HTTPS (TLS 1.3 · Let's Encrypt auto)
                 ▼
         [Caddy Reverse Proxy + rate limiting]
          ├── /      ──► Flutter Web (statique)
          └── /api/* ──► [Spring Boot API :8080] (réseau Docker interne)
                                │
                                ▼
                         [PostgreSQL 16] (non exposé, volume chiffré, RLS)
                                │
              ┌─────────────────┴──────────────────┐
              ▼                                     ▼
   [Archivage WAL continu (PITR)]        [pg_dump quotidien chiffré]
              └──────────────► Stockage hors-serveur ◄┘
```

- **PostgreSQL jamais exposé publiquement**, rôles séparés : `sigeb_owner` (migrations), `sigeb_app` (droits limités, sans UPDATE/DELETE sur l'audit), `sigeb_readonly` (statistiques).
- **Chiffrement** : TLS en transit ; **volume PostgreSQL chiffré** (LUKS/chiffrement disque du VPS) ; dumps chiffrés (`age`/`gpg`) ; secrets de clés QR chiffrés en base.
- **Secrets hors du code** : `JWT_SECRET`, `MASTER_KEY`, `DB_PASSWORD` via variables d'environnement (`.env` ignoré par Git).
- **Sauvegarde 3-2-1 avec objectifs explicites** :

| Objectif | Valeur cible |
| :--- | :--- |
| **RPO** (perte de données max) | ≤ 5 minutes (archivage WAL / PITR avec pgBackRest ou WAL-G) |
| **RTO** (temps de reprise) | ≤ 4 heures (restauration documentée) |
| **Test de restauration** | Automatisé chaque mois (`restore_test.sh`) + rapport |

- **Observabilité** : logs **JSON** avec `correlationId`, métriques Micrometer, `/actuator/health`, alertes email/Telegram (Uptime Kuma ou Grafana).
- **Conformité & données personnelles** (loi camerounaise sur la protection des données) :
  - Registre de traitement et politique de conservation documentés.
  - Journal de **chaque consultation** de la recherche nationale (qui, quoi, motif).
  - Droit d'accès/rectification traité via avenant tracé.
  - Données minimales dans les scans anonymes.

---

## 6. CI/CD & Qualité

| Pipeline | Étapes |
| :--- | :--- |
| **backend.yml** | Compilation Java 21 → Flyway sur PostgreSQL **Testcontainers** → tests unitaires/intégration (IUN, RLS, idempotence, QR) → ArchUnit → image Docker versionnée |
| **frontend.yml** | `flutter analyze` → `flutter test` → régénération du client OpenAPI (échec si dérive) → `flutter build web` |
| **release.yml** | Tag Git → déploiement **staging** → validation → promotion **prod** (images immuables, retour arrière = redéployer le tag précédent) |

---

## 7. Ordre d'Implémentation Recommandé (12 semaines)

1. **S1** : Monorepo, CI minimale, `docker-compose.dev.yml`, `V1__init_schema.sql` (avec RLS et droits audit), `V2__seed_cameroun.sql`.
2. **S2 – S3** : `auth` (JWT, refresh haché, Argon2id, 2FA, verrouillage) + **`audit` (chaîné, dès maintenant)** + `referentiel` + tests RBAC/RLS.
3. **S4 – S6** : `bapteme` (doublons, compteur IUN par paroisse/année, scellement, avenants, idempotence, `version`) + `carte` (`template_version`, duplicata, révocation, PDF/QR) + `verification` (KeyRing `kid`, scan public limité, recherche nationale tracée).
4. **S7 – S8** : `mention`, `statistique`, vérification nocturne de la chaîne d'audit, observabilité (logs JSON, métriques).
5. **S9 – S10** : Écrans Flutter (Mobile + Web) sur le client OpenAPI généré, bilinguisme FR/EN, brouillons locaux, cookie HttpOnly côté web.
6. **S11 – S12** : Tests bout-en-bout, staging, **PITR + test de restauration**, `docker-compose.prod.yml`, formation, lancement pilote.

---

## 8. Registre des Décisions Révisées (v1 → v2)

| Sujet | v1 | v2 |
| :--- | :--- | :--- |
| Clé QR | Secret unique « rotatif » | Clés versionnées (`kid`), table `cle_signature`, format `v2` |
| Vérification QR | Non protégée | Rate limiting, réponse minimale en anonyme, scans tracés |
| Cloisonnement | `TenantContext` seul | + Row-Level Security PostgreSQL |
| Audit | Convention applicative | `REVOKE`, chaînage de hash, vérification nocturne, dès S2 |
| Sauvegardes | `pg_dump` quotidien | + WAL/PITR, RPO/RTO, test de restauration automatisé |
| IUN | `FOR UPDATE` non précisé | Compteur par (paroisse, année) |
| Doubles envois | Non traité | `Idempotency-Key` + verrouillage optimiste |
| Carte PDF | Régénération à la volée | + `template_version` figé |
| Comptes | PIN non précisé | Argon2id, verrouillage progressif, 2FA rôles sensibles |
| Jetons web | « Session sécurisée » | Cookie HttpOnly + access token en mémoire |
| Qualité | Testcontainers | + ArchUnit/Modulith, CI/CD, client OpenAPI généré, staging |
| Hors-ligne | Non mentionné | Hors MVP ; brouillon local uniquement |
| Conformité | Non traitée | Politique de conservation, journal de consultation, chiffrement au repos |

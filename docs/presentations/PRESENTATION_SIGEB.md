---
marp: true
theme: default
size: 16:9
paginate: true
header: "SIGEB — Église Catholique du Cameroun"
footer: "Document de Cadrage · Confidentiel"
style: |
  section {
    background-color: #FAFBFC;
    color: #1E293B;
    font-family: 'Segoe UI', system-ui, -apple-system, Roboto, sans-serif;
    font-size: 25px;
    padding: 46px 58px;
    line-height: 1.42;
  }
  header, footer {
    color: #64748B;
    font-size: 15px;
    letter-spacing: 0.3px;
  }
  h1 {
    color: #1B2A4A;
    font-size: 40px;
    font-weight: 800;
    border-bottom: 3px solid #C59B27;
    padding-bottom: 8px;
    margin-bottom: 18px;
  }
  h2 {
    color: #1B2A4A;
    font-size: 28px;
    font-weight: 700;
    margin-top: 6px;
    margin-bottom: 10px;
  }
  strong {
    color: #1B2A4A;
  }
  ul li {
    margin-bottom: 10px;
  }
  ul li::marker {
    color: #C59B27;
    font-weight: bold;
  }
  blockquote {
    background: #F1F5F9;
    border-left: 5px solid #C59B27;
    padding: 12px 18px;
    margin: 14px 0 0 0;
    color: #1E293B;
    border-radius: 0 8px 8px 0;
    font-size: 22px;
  }
  table {
    display: table;
    width: 100%;
    border-collapse: collapse;
    font-size: 21px;
    margin-top: 10px;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
  }
  th {
    background-color: #1B2A4A;
    color: #FFFFFF;
    font-weight: 700;
    padding: 10px 14px;
  }
  td {
    padding: 9px 14px;
    border-bottom: 1px solid #E2E8F0;
    background-color: #FFFFFF;
  }
  tr:nth-child(even) td {
    background-color: #F8FAFC;
  }
  code {
    background: #E2E8F0;
    color: #1B2A4A;
    padding: 2px 7px;
    border-radius: 4px;
    font-size: 0.9em;
  }
  pre {
    background: #1B2A4A;
    color: #F8FAFC;
    border-radius: 10px;
    padding: 20px 24px;
    font-size: 21px;
    line-height: 1.5;
    border-left: 6px solid #C59B27;
  }
  pre code {
    background: transparent;
    color: #F8FAFC;
    padding: 0;
  }
  section.lead {
    background: linear-gradient(135deg, #1B2A4A 0%, #0F172A 100%);
    color: #FFFFFF;
    justify-content: center;
    text-align: left;
    padding: 68px 78px;
  }
  section.lead h1 {
    color: #FFFFFF;
    font-size: 55px;
    border-bottom: 4px solid #C59B27;
    padding-bottom: 18px;
    margin-bottom: 24px;
  }
  section.lead h2 {
    color: #E2C15A;
    font-size: 31px;
    font-weight: 500;
    margin-bottom: 30px;
  }
  section.lead p {
    color: #CBD5E1;
    font-size: 24px;
  }
  section.lead strong {
    color: #FFFFFF;
  }
  section.lead blockquote {
    background: rgba(255, 255, 255, 0.08);
    border-left: 5px solid #C59B27;
    color: #F1F5F9;
    font-size: 22px;
  }
  section.closing {
    background: linear-gradient(135deg, #1B2A4A 0%, #0F172A 100%);
    color: #F1F5F9;
    font-size: 23px;
    padding: 46px 60px;
  }
  section.closing h1 {
    color: #FFFFFF;
    font-size: 39px;
    border-bottom: 3px solid #C59B27;
  }
  section.closing strong {
    color: #E2C15A;
  }
  section.closing ol li {
    margin-bottom: 10px;
  }
  section.closing ol li::marker {
    color: #C59B27;
    font-weight: bold;
  }
  section.closing blockquote {
    background: rgba(255, 255, 255, 0.08);
    border-left: 5px solid #C59B27;
    color: #F8FAFC;
    font-size: 21px;
    margin-top: 18px;
  }
  section.closing code {
    background: rgba(255, 255, 255, 0.14);
    color: #FFFFFF;
  }
  section.closing header, section.closing footer {
    color: #94A3B8;
  }
  section.compact table {
    font-size: 19.5px;
  }
  section.compact td, section.compact th {
    padding: 8px 12px;
  }
  img[src^="assets/"] {
    display: block;
    margin: 8px auto 12px auto;
    border-radius: 10px;
  }
  td img, th img, blockquote img, li img {
    display: inline-block !important;
    margin: 0 !important;
    vertical-align: -0.15em;
    border-radius: 0;
  }
---

<!-- _class: lead -->
<!-- _header: "" -->
<!-- _footer: "" -->
<!-- _paginate: false -->

# SIGEB
## Système Informatique de Gestion des Cartes de Baptême

**Église Catholique du Cameroun** — Document de Cadrage

> **Architecture** : Backend **Spring Boot** · Frontend unifié **Flutter** (Mobile Android/iOS & Web)

---

# 1. Contexte & Objectif

| Situation actuelle (Papier) | Vision cible (SIGEB National) |
| :--- | :--- |
| Registres physiques exposés aux pertes et dégradations | **Base nationale centralisée**, sécurisée et sauvegardée |
| Cartes cartonnées faciles à perdre ou falsifier | **Cartes & extraits infalsifiables** avec QR Code signé |
| Déplacements coûteux vers la paroisse d'origine | **Délivrance et vérification à distance** sur tout le territoire |
| Mise à jour manuelle lente des mentions marginales | **Synchronisation inter-paroisses** fluide et tracée |

> **Ambition** : Moderniser et sécuriser le registre sacramentel tout en respectant strictement la hiérarchie et le droit canonique.

---

# 2. Modèle Hiérarchique Ecclésial

![h:305 Modèle hiérarchique ecclésial](assets/hierarchie-ecclesiale.png)

- **Autonomie locale** : Chaque acte appartient à sa paroisse de célébration.
- **Supervision ascendante** : Consolidation statistique et contrôle au niveau diocésain et national.
- **Cloisonnement** : Aucune consultation libre hors périmètre sans motif tracé.

---

# 3. Règles Métier Clés (Droit Canonique)

- **Identifiant Unique National (IUN)** : Ex. `CM-YDE-P012-2026-00458`, systématiquement lié à la **référence papier** (`Tome / Page / N° d'ordre`).
- **Double contrôle & Immuabilité** :
  - Saisie par le secrétariat → **Validation / Scellement par le Curé**.
  - Un acte validé ne se modifie plus directement : toute erreur passe par une **rectification tracée** (avenant validé).
- **Pivot sacramentel (Can. 535 §2)** : L'acte de baptême centralise **Confirmation**, **Mariage** (ou nullité), **Ordination** et **Vœux perpétuels**.
- **Reprise de l'historique** : Enregistrement rétroactif d'anciens actes avec leurs **références exactes au registre papier** (`Tome / Page / N°`), validé par le Curé.

---

# 4. Mentions Marginales & Vérification QR

![h:385 Flux des mentions marginales et vérification QR](assets/flux-mentions-qr.png)

> **Impact direct** : Lors d'une préparation au mariage, un prêtre vérifie en **quelques secondes** l'authenticité du baptême et l'absence d'empêchement matrimonial.

---

<!-- _class: compact -->
# 5. Acteurs & Permissions (RBAC)

| Rôle | Niveau | Responsabilités principales |
| :--- | :--- | :--- |
| **Admin National (CENC)** | National | Référentiel provinces/diocèses, supervision technique, statistiques nationales |
| **Chancelier / Admin Diocésain** | Diocèse | Création des paroisses, gestion des accès curés, rectifications majeures |
| **Curé / Administrateur** | Paroisse | Validation et scellement des actes, gestion des secrétaires, délivrance officielle |
| **Secrétaire Paroissial** | Paroisse | Saisie des dossiers, numérisation des archives, impression *(ne scelle pas)* |
| **Fidèle / Parent** *(optionnel)* | Personnel | Consultation de sa carte numérique, demande d'extrait ou duplicata |

---

# 6. Périmètre Fonctionnel : MVP vs Phase 2

| Phase 1 — MVP (Cœur du système) | Phase 2 — Évolutions |
| :--- | :--- |
| Référentiel national & gestion des rôles (RBAC) | **Espace Fidèle** (carte mobile + demandes en ligne) |
| Enregistrement baptêmes + validation Curé | **Paiement mobile** (MTN MoMo / Orange Money) |
| Génération à la volée Cartes/Extraits + **données QR en BD** | **Mode hors-ligne** (zones rurales + sync auto) |
| Vérification QR en base & recherche nationale tracée | **Tableaux de bord pastoraux** avancés |
| Mentions marginales inter-paroisses & audit | |
| Reprise d'historique *(réf. registre papier)* | |

---

# 7. Spécificités du Contexte Camerounais

- **Bilinguisme (Français / Anglais)** :
  - Couverture complète des diocèses francophones et anglophones (interface et documents bilingues dès la conception).
- **Confidentialité & Protection des données** :
  - Données sensibles (filiation, adoption, statut matrimonial) protégées par chiffrement (HTTPS + base sécurisée) et **journal d'audit intégral**.
- **Multi-supports & Connectivité** :
  - Pensé *mobile-first* (smartphones/tablettes sur le terrain) et adapté aux postes fixes via **Flutter Web** (secrétariats et chancelleries).

---

# 8. Architecture Technique Retenue

| Couche | Technologie | Rôle |
| :--- | :--- | :--- |
| **Frontend unifié** | **Flutter** (Android, iOS, Web) | Base de code unique mobile & bureau, scan QR intégré |
| **Backend API** | **Spring Boot** (Java 21) | API REST, règles canoniques, Spring Security (JWT/RBAC), rendu à la volée |
| **Base de données** | **PostgreSQL** *(sans gestionnaire de fichiers)* | Actes, **cartes & duplicatas imprimés**, jetons QR et piste d'audit |
| **Sécurité Cartes** | **Jeton signé + vérification BD** | Contrôle instantané de la carte imprimée et de l'état libre |

---

<!-- _class: closing -->
# 9. Points à Valider Ensemble

1. **Nom officiel** : Confirmez-vous `SIGEB` ?
2. **Accès Fidèles** : Inclus dès la V1 ou démarrage réservé au clergé & secrétariats ?
3. **Bilinguisme FR / EN** : Intégré dès la V1 ?
4. **Mode hors-ligne** : Prioritaire V1 (zones rurales) ou Phase 2 ?
5. **Paiement Mobile Money (MoMo / OM)** : Inclus ou hors périmètre V1 ?
6. **Modèle de carte** : Existe-t-il un gabarit papier CENC officiel à reproduire ?

> **Prochaine étape après validation** : Modèle de données (MCD) · Architecture détaillée · Initialisation Spring Boot & Flutter

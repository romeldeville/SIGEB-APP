---
marp: true
theme: default
size: 16:9
paginate: true
header: "SIGEB — Spécification des Cas d'Utilisation"
footer: "Église Catholique du Cameroun · Spring Boot + Flutter"
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
  ul li, ol li {
    margin-bottom: 10px;
  }
  ul li::marker, ol li::marker {
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
    font-size: 21px;
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
  section.lead {
    background: linear-gradient(135deg, #1B2A4A 0%, #0F172A 100%);
    color: #FFFFFF;
    justify-content: center;
    text-align: left;
    padding: 68px 78px;
  }
  section.lead h1 {
    color: #FFFFFF;
    font-size: 54px;
    border-bottom: 4px solid #C59B27;
    padding-bottom: 16px;
    margin-bottom: 22px;
  }
  section.lead h2 {
    color: #E2C15A;
    font-size: 30px;
    font-weight: 500;
    margin-bottom: 28px;
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
  section.closing blockquote {
    background: rgba(255, 255, 255, 0.08);
    border-left: 5px solid #C59B27;
    color: #F8FAFC;
    font-size: 21px;
    margin-top: 18px;
  }
  section.closing header, section.closing footer {
    color: #94A3B8;
  }
  section.compact table {
    font-size: 19px;
  }
  section.compact td, section.compact th {
    padding: 7px 12px;
  }
  section.matrix table {
    display: table !important;
    width: 100% !important;
    font-size: 17px;
    table-layout: fixed;
  }
  section.matrix th,
  section.matrix td {
    padding: 7px 10px;
    white-space: nowrap;
    vertical-align: middle;
  }
  section.matrix th:nth-child(1),
  section.matrix td:nth-child(1) {
    width: 44%;
    text-align: left !important;
  }
  section.matrix th:nth-child(2),
  section.matrix td:nth-child(2),
  section.matrix th:nth-child(3),
  section.matrix td:nth-child(3),
  section.matrix th:nth-child(4),
  section.matrix td:nth-child(4),
  section.matrix th:nth-child(5),
  section.matrix td:nth-child(5) {
    width: 14%;
    text-align: center !important;
    padding: 7px 6px;
  }
  section.matrix blockquote {
    font-size: 17.5px;
    padding: 9px 14px;
    margin-top: 10px;
  }
  img[src^="assets/"] {
    display: block;
    margin: 6px auto 10px auto;
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

# SIGEB — Cas d'Utilisation
## Spécification Fonctionnelle & Matrice RBAC

**Système National de Gestion des Cartes de Baptême** · Église Catholique du Cameroun

> **15 cas d'utilisation MVP** répartis en **5 modules** + **3 évolutions Phase 2**

---

<!-- _class: compact -->
# 1. Acteurs du Système

| Acteur | Niveau | Rôle & Périmètre |
| :--- | :--- | :--- |
| **Admin National (CENC)** | National | Référentiel provinces/diocèses, comptes chanceliers, audit & statistiques nationales |
| **Chancelier Diocésain** | Diocèse | Paroisses et comptes curés, rectifications majeures, suivi diocésain |
| **Curé / Administrateur** | Paroisse | **Acteur pivot** : scellement des actes, gestion secrétaires, mentions marginales, délivrance |
| **Secrétaire Paroissial** | Paroisse | Saisie des brouillons, reprise d'archives, impression *(ne scelle jamais seul un acte)* |
| **Fidèle / Parent** `[V2]` | Personnel | Consultation de sa carte numérique et demande d'extrait en ligne |

---

# 2. Vue Globale des Cas d'Utilisation (MVP)

![h:505 Vue globale des cas d'utilisation SIGEB](assets/diagramme-cas-utilisation.png)

---

<!-- _class: compact -->
# 3. Module 1 — Accès & Référentiel Ecclésial

| ID | Cas d'utilisation | Acteur | Règles clés |
| :--- | :--- | :--- | :--- |
| **UC-01** | **Authentification & session** | Tous | JWT avec périmètre (`national`, `dioceseId`, `paroisseId`), interface **FR / EN**, verrouillage après 5 échecs |
| **UC-02** | **Provinces, Diocèses & Chanceliers** | Admin National | Gestion des 5 provinces et codes diocésains uniques (`YDE`, `DLA`, `BMD`…) immuables après usage |
| **UC-03** | **Paroisses & Curés** | Chancelier | Création des paroisses/chapelles, affectation ou mutation d'un curé sans perte d'historique |
| **UC-04** | **Comptes Secrétaires** | Curé | Création et suspension des secrétaires paroissiaux (droits limités à la saisie et l'impression) |

---

# 4. Module 2 — Cycle de Vie d'un Acte de Baptême

![h:370 Cycle de vie d'un acte de baptême SIGEB](assets/cycle-vie-acte.png)

> **Principe canonique** : Un acte scellé par le Curé devient **immuable** ; toute correction ultérieure génère un **avenant historisé** sans jamais effacer l'original.

---

<!-- _class: compact -->
# 5. Module 2 — Détail des Cas d'Utilisation (`UC-05` à `UC-08`)

- **UC-05 — Saisir un acte (Brouillon)** *(Secrétaire / Curé)* :
  - Données canoniques (baptisé, filiation, parrain/marraine, célébrant, date/lieu, réf. `Tome/Page/N°`) + alerte anti-doublon automatique.
- **UC-06 — Valider & sceller** *(Curé uniquement)* :
  - Confirmation par PIN/mot de passe → génération de l'**IUN** (`CM-YDE-P012-2026-00458`) et de la **signature cryptographique QR**.
- **UC-07 — Reprise d'archives historiques** *(Secrétaire / Curé)* :
  - Saisie d'actes anciens avec les **références exactes du registre papier** (`Tome/Page/N°`, sans stockage de fichiers), vérifiée par le Curé.
- **UC-08 — Rectification (Avenant tracé)** *(Curé / Chancelier)* :
  - Erreur orthographique mineure validée par le Curé ; changement majeur (filiation, identité) soumis au **Chancelier**.

---

<!-- _class: compact -->
# 6. Module 3 — Cartes, Extraits & Vérification QR

| ID | Cas d'utilisation | Fonctionnement & Contrôle |
| :--- | :--- | :--- |
| **UC-09** | **Générer Carte / Duplicata** *(Curé, Secrétaire)* | Rendu **à la volée** (sans fichiers) · Chaque **originale et chaque duplicata** est enregistré en BD (données imprimées, n° duplicata, motif, jeton QR) |
| **UC-10** | **Vérifier via Scan QR** *(Curé, Secrétaire, Chancelier)* | Scan mobile → contrôle cryptographique + vérification en BD (**authentique**, **révoquée/remplacée par duplicata**, ou **état libre à jour**) |
| **UC-11** | **Recherche nationale tracée** *(Curé, Chancelier)* | **Motif pastoral obligatoire** (ex. mariage) + min. 2 critères · Affiche uniquement le résumé utile · **Journalisation systématique** |

---

# 7. Module 4 — Mentions Marginales Inter-Paroisses

![h:375 Flux des mentions marginales et vérification QR](assets/flux-mentions-qr.png)

> **UC-12 (Paroisse B)** déclare Confirmation, Mariage, Nullité, Ordination ou Vœux → **UC-13 (Paroisse A)** approuve et inscrit en marge, mettant à jour l'état libre immédiatement.

---

# 8. Module 5 — Audit Immuable & Pilotage

- **UC-14 — Journal d'audit (`append-only`)** :
  - Trace chaque action critique (`CREATION`, `SCELLEMENT`, `IMPRESSION`, `SCAN_QR`, `RECHERCHE_NATIONALE`, `RECTIFICATION`, `MENTION`).
  - Lecture seule stricte par périmètre (Paroisse / Diocèse / National) : **aucune suppression possible**.
- **UC-15 — Statistiques sacramentelles** :
  - Indicateurs consolidés par période : baptêmes célébrés, archives numérisées, mentions enregistrées, extraits délivrés, paroisses actives.
  - Export **PDF / CSV** pour les rapports pastoraux annuels.

---

<!-- _class: matrix -->
# 9. Matrice des Permissions RBAC (Spring Security)

| Cas d'utilisation | Admin National | Chancelier | Curé | Secrétaire |
| :--- | :---: | :---: | :---: | :---: |
| **UC-01** Authentification & session FR/EN | ✅ | ✅ | ✅ | ✅ |
| **UC-02** Provinces, Diocèses & Chanceliers | ✅ | — | — | — |
| **UC-03** Paroisses & comptes Curés | 👁️ | ✅ | — | — |
| **UC-04** Comptes Secrétaires paroissiaux | — | 👁️ | ✅ | — |
| **UC-05 · UC-07** Saisie brouillon & reprise d'archives | — | — | ✅ | ✅ |
| **UC-06 · UC-12 · UC-13** Scellement & mentions marginales | — | — | ✅ | — |
| **UC-08** Rectification par avenant tracé | — | ✅ | ✅ | — |
| **UC-09 · UC-10** Cartes/Extraits PDF & Scan QR | — | ✅ | ✅ | ✅ |
| **UC-11** Recherche nationale tracée | — | ✅ | ✅ | — |
| **UC-14 · UC-15** Journal d'audit & statistiques | ✅ | ✅ | ✅ | — |

> **✅** Autorisé *(périmètre propre)* · **👁️** Lecture seule · **—** Non autorisé · *UC-08 : mineure Curé, majeure Chancelier · UC-09 : acte scellé.*

---

<!-- _class: closing -->
# 10. Évolutions Phase 2 (`V2`) & Suite Technique

1. **UC-16 `[V2]` — Espace Fidèle** : Accès mobile sécurisé (OTP) pour consulter sa carte numérique et son QR Code.
2. **UC-17 `[V2]` — Demande d'extrait & Mobile Money** : Commande en ligne avec paiement **MTN MoMo / Orange Money** et délivrance PDF validée par le Curé.
3. **UC-18 `[V2]` — Mode hors-ligne** : Saisie des brouillons sans réseau en zone rurale avec synchronisation automatique chiffrée.

> **Prochaine étape de conception** : Modèle Conceptuel de Données (**MCD / Schéma PostgreSQL**) et spécification des **API REST Spring Boot**.

---
marp: true
theme: default
size: 16:9
paginate: true
header: "SIGEB — Budget & Délais"
footer: "Église Catholique du Cameroun · Proposition Budgétaire (FCFA)"
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
    margin-bottom: 8px;
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
    font-size: 24px;
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

# SIGEB — Budget & Délais
## Plan de Réalisation, Coûts & Modèle de Pérennité

**Système National de Gestion des Cartes de Baptême** · Église Catholique du Cameroun

> Proposition budgétaire en **FCFA (XAF)** · Socle **Spring Boot + Flutter (Mobile & Web)**

---

# 1. Synthèse Exécutive (Chiffres Clés)

| Indicateur | Valeur cible | Détail |
| :--- | :--- | :--- |
| **Durée totale MVP** | **3 mois** *(12 semaines)* | De la conception au lancement pilote |
| **Budget Réalisation (CAPEX)** | **3 800 000 FCFA** | Conception, Dev, Sécurité, Déploiement & Pilote |
| **Exploitation annuelle (OPEX)** | **1 060 000 FCFA / an** | Serveur Cloud, sauvegardes, stores & maintenance |
| **Phase 2 — Évolutions (option)** | **+4 à 6 sem.** · **1 500 000 FCFA** | Espace Fidèle, Mobile Money, mode hors-ligne |

> **Approche** : Mise en service rapide sur **1 diocèse pilote** (5 à 10 paroisses) en fin de **Mois 3**, puis généralisation progressive.

---

# 2. Macro-Planning (12 semaines · 3 mois)

![h:395 Macro-planning SIGEB sur 12 semaines](assets/macro-planning.png)

> **Méthode** : Livraisons hebdomadaires visibles avec validation continue des règles canoniques et du gabarit de carte.

---

<!-- _class: compact -->
# 3. Jalons & Livrables (S1 à S12)

| Période | Jalon clé | Livrables concrets |
| :--- | :--- | :--- |
| **S1 – S2** | **Cadrage & Maquettes** | Règles canoniques figées, MCD, écrans Flutter (Mobile & Web), gabarit carte |
| **S3 – S4** | **Socle & Référentiel** | API Spring Boot, sécurité JWT/RBAC, hiérarchie Provinces → Paroisses |
| **S5 – S7** | **Baptêmes, PDF & QR** | Saisie actes, validation Curé, génération Carte/Extrait PDF + QR Code signé |
| **S8 – S9** | **Inter-paroisses & Audit** | Mentions marginales, recherche nationale tracée, reprise d'archives, FR/EN |
| **S10 – S11** | **Recette & Déploiement** | Tests sécurité, mise en ligne serveur sécurisé, publication stores |
| **S12** | **Pilote & Go-Live** | Formation curés/secrétaires (1er diocèse), mise en production pilote |

---

<!-- _class: compact -->
# 4. Budget de Réalisation — Phase 1 MVP (CAPEX)

| Poste | Montant (FCFA) | Périmètre inclus |
| :--- | ---: | :--- |
| **Cadrage, Modèle de données & UI/UX** | 350 000 | Spécifications, MCD, maquettes Mobile & Web, gabarit officiel de carte |
| **Backend Spring Boot** | 1 500 000 | API REST, RBAC ecclésial, mentions marginales, PDF/QR signé, journal d'audit |
| **Frontend Flutter (Mobile + Web)** | 1 500 000 | App Android/iOS, interface Web bureau, scanner QR intégré, interface FR/EN |
| **Sécurité, Déploiement & Pilote** | 450 000 | Configuration serveur HTTPS/sauvegardes, guides utilisateurs, formation 1er diocèse |
| **TOTAL MVP (Forfait clé en main)** | **3 800 000** | *Hors frais récurrents d'hébergement et comptes stores* |

> **Échéancier proposé** : **40 %** au démarrage (S1) · **30 %** à la version Beta (S8) · **30 %** au lancement pilote (S12).

---

<!-- _class: compact -->
# 5. Coûts Récurrents (OPEX) & Phase 2

## Exploitation annuelle (continuité & sécurité)

| Poste annuel | Montant / an (FCFA) | Détail |
| :--- | ---: | :--- |
| **Serveur Cloud & Sauvegardes** | 300 000 | VPS dédié performant (App + PostgreSQL + backups chiffrés automatiques) |
| **Domaine, SSL & Notifications** | 80 000 | Nom de domaine national, certificats HTTPS, envoi d'emails/alertes |
| **Comptes Stores (Android / iOS)** | 80 000 | Licences de publication Google Play & Apple Developer |
| **Maintenance & Support Technique** | 600 000 | Correctifs sécurité, mises à jour Flutter/OS, assistance chancelleries |
| **TOTAL OPEX ANNUEL** | **1 060 000** | *Soit environ **88 000 FCFA / mois** pour tout le réseau* |

> **Phase 2 (optionnelle, +4 à 6 semaines)** : **1 500 000 FCFA** *(Espace Fidèle, MTN MoMo / Orange Money, mode hors-ligne, tableaux de bord).*

---

# 6. Financement & Autofinancement National

- **Investissement initial (3 800 000 FCFA)** :
  - Financement direct par le **diocèse pilote**, la **CENC** ou un cofinancement interdiocésain.
- **Autofinancement de l'exploitation (1 060 000 FCFA / an)** :
  - **200 paroisses × 5 500 FCFA / an = 1 100 000 FCFA / an** → exploitation couverte à 100 %.
  - **500 paroisses × 2 500 FCFA / an = 1 250 000 FCFA / an** → excédent réinvesti dans les évolutions.
  - En Phase 2 : contribution symbolique sur les demandes d'extraits numériques (Mobile Money).

> **Bilan** : Un coût annuel par paroisse inférieur à **500 FCFA / mois** à l'échelle nationale.

---

# 7. Options & Facteurs d'Impact

| Option / Facteur | Délai additionnel | Budget additionnel | Recommandation |
| :--- | :---: | :---: | :--- |
| **Mode hors-ligne complet dès la V1** | +2 sem. | +500 000 FCFA | Reporter en Phase 2 |
| **Paiement MTN MoMo / Orange Money** | +2 sem. | +500 000 FCFA | Reporter en Phase 2 |
| **Espace Fidèle grand public** | +2 sem. | +500 000 FCFA | Démarrer par clergé & secrétariats |
| **Validation tardive du gabarit de carte** | +1 sem. | 0 FCFA | Valider le modèle dès S2 |

---

<!-- _class: closing -->
# 8. Décisions pour Démarrer

1. **Valider le périmètre MVP et le budget** (**3 800 000 FCFA** · **12 semaines**).
2. **Désigner le diocèse pilote** (5 à 10 paroisses pour le lancement à S12).
3. **Nommer 1 référent diocésain** pour valider le modèle de carte et suivre les jalons.
4. **Fixer la date de démarrage (S1)** pour lancer immédiatement la conception.

> **Livrable dès S2** : Modèle de données (MCD), maquettes complètes Mobile & Web et prototype de carte de baptême.

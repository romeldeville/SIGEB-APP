# Document de Cadrage — Système National de Gestion des Cartes de Baptême (SIGEB)

> **Périmètre** : Église Catholique du Cameroun (niveau national)  
> **Stack retenue** : Backend **Spring Boot** · Frontend unique **Flutter** (Mobile Android/iOS + Web)

---

## 1. Objectif du projet

Remplacer la gestion exclusivement papier des registres et cartes de baptême par une **plateforme nationale centralisée, sécurisée et vérifiable** permettant :
- D'enregistrer et sécuriser les actes de baptême dans chaque paroisse.
- D'émettre des cartes et extraits de baptême **infalsifiables** (avec QR Code).
- D'éliminer les déplacements interurbains/interdiocésains pour récupérer un certificat de baptême (mariage, confirmation, séminaire, parrainage).
- D'assurer la mise à jour automatique des **mentions marginales** entre paroisses.

---

## 2. Organisation hiérarchique (Modèle ecclésial)

Le système reflète la structure territoriale de l'Église au Cameroun :

```text
National (CENC)
 └── Province Ecclésiastique (5 : Yaoundé, Douala, Bamenda, Garoua, Bertoua)
      └── Diocèse / Archidiocèse (Chancellerie)
           └── Paroisse (et missions/chapelles rattachées)
```

Chaque donnée est rattachée à sa paroisse d'origine, avec visibilité ascendante (statistiques et contrôle) pour le diocèse et le niveau national.

---

## 3. Règles métier fondamentales (Droit canonique & pratiques locales)

1. **Identifiant Unique National (IUN)** : Chaque acte reçoit un identifiant unique (ex. `CM-YDE-P012-2026-00458`) couplé à la **référence papier** (`Tome / Page / N° d'ordre`) pour garantir la correspondance avec les registres physiques.
2. **Double contrôle & Immuabilité** :
   - Saisie par le secrétariat → **Validation / Scellement** par le Curé (ou prêtre habilité).
   - Un acte validé **n'est plus modifiable directement** : toute correction (erreur de nom, date) passe par une **rectification tracée** (avenant validé par le curé ou la chancellerie).
3. **Pivot sacramentel (Mentions marginales)** : Conformément au droit canonique (can. 535 §2), l'acte de baptême centralise la vie sacramentelle du fidèle :
   - **Confirmation** (date, lieu, ministre)
   - **Mariage** (conjoint, date, lieu) / déclaration de nullité
   - **Ordination** (diaconat, presbytérat) ou **Vœux perpétuels**
   - *Flux inter-paroisses* : Lorsqu'une paroisse B célèbre un mariage ou une confirmation, elle transmet numériquement la mention qui met à jour l'acte de la paroisse A (après validation).
4. **Authenticité par QR Code & traçabilité des duplicatas (100 % BD)** : Chaque carte originale et chaque **duplicata** est généré à la volée (sans stockage de fichiers) et enregistré dans PostgreSQL (données imprimées, n° de duplicata, motif, jeton/signature QR, statut `ACTIVE / REVOQUEE`). Un duplicata pour perte/vol révoque l'ancienne carte et le scan QR le signale immédiatement.
5. **Reprise de l'existant** : Possibilité d'enregistrer des actes antérieurs au système avec leur référence exacte au registre physique (`Tome / Page / N° d'ordre`), validée par le Curé.

---

## 4. Acteurs & Permissions (RBAC)

| Rôle | Niveau | Responsabilités clés |
| :--- | :--- | :--- |
| **Admin National (CENC)** | National | Référentiel des provinces/diocèses, supervision technique, statistiques nationales. |
| **Chancelier / Admin Diocésain** | Diocèse | Création des paroisses, gestion des comptes curés, validation des rectifications majeures, reporting diocésain. |
| **Curé / Administrateur** | Paroisse | Validation et scellement des actes, gestion des secrétaires paroissiaux, délivrance officielle des extraits/cartes. |
| **Secrétaire Paroissial** | Paroisse | Saisie des dossiers, préparation des baptêmes, numérisation des anciens registres, impression (sans pouvoir sceller un acte). |
| **Fidèle / Parent** *(optionnel V1/V2)* | Personnel | Consultation de sa carte numérique, demande de duplicata ou d'extrait en ligne. |

---

## 5. Périmètre fonctionnel

### Phase 1 — MVP (Cœur du système)
- **Référentiel & Comptes** : Gestion hiérarchique (Provinces → Diocèses → Paroisses) et authentification sécurisée par rôle.
- **Gestion des Baptêmes** : Saisie complète (baptisé, filiation, parrain/marraine, célébrant, date, lieu, réf. registre) + circuit de validation curé.
- **Cartes & Extraits (sans gestionnaire de fichiers)** : Génération à la volée de la carte de baptême et de l'extrait officiel A4, avec enregistrement en base de données des **données imprimées et du jeton de vérification QR**.
- **Vérification & Recherche** : Scan QR Code instantané (vérifié contre les données de la carte en base) et recherche nationale contrôlée (accès tracé).
- **Mentions marginales** : Ajout et notification inter-paroisses (Confirmation, Mariage, Ordination).
- **Reprise d'historique** : Saisie rétroactive d'anciens actes avec référence exacte du registre papier (`Tome / Page / N°`).
- **Journal d'audit** : Traçabilité intégrale (qui a saisi, validé, consulté, imprimé ou rectifié un acte).

### Phase 2 — Évolutions envisagées
- **Espace Fidèle** : Accès mobile personnel à sa carte numérique et suivi des demandes d'extraits.
- **Paiement mobile** : Règlement des frais d'extraits/duplicatas via **MTN MoMo / Orange Money**.
- **Mode hors-ligne** : Saisie en zone rurale sans connexion et synchronisation automatique au retour du réseau.
- **Tableaux de bord pastoraux** : Statistiques sacramentelles consolidées (diocèse / national).

---

## 6. Exigences transverses (Contexte camerounais)

- **Bilinguisme (Français / Anglais)** : Indispensable pour couvrir l'ensemble du territoire (diocèses francophones et anglophones) — interface et documents bilingues.
- **Confidentialité & Sécurité** :
  - Données sensibles (filiation, adoption, statut matrimonial) → chiffrement en transit (HTTPS) et au repos.
  - Cloisonnement strict : aucune consultation libre de la base nationale sans motif enregistré dans l'audit.
- **Multi-supports** : Application pensée *mobile-first* (tablettes/smartphones pour les prêtres et secrétaires) et parfaitement utilisable sur ordinateur via **Flutter Web** (bureaux paroissiaux et chancelleries).

---

## 7. Architecture technique retenue

| Couche | Choix | Rôle |
| :--- | :--- | :--- |
| **Backend** | **Spring Boot** (Java 21) | API REST sécurisée, logique métier canonique, rendu d'impression à la volée, gestion des rôles (Spring Security / JWT). |
| **Base de données** | **PostgreSQL** | Stockage unique : actes, **données des cartes imprimées**, jetons/signatures QR et journal d'audit *(aucun gestionnaire de fichiers)*. |
| **Frontend unifié** | **Flutter** (Mobile + Web) | Codebase unique pour **Android / iOS** et **Web** (interface adaptative mobile & bureau, scan QR natif). |
| **Vérification QR** | **Jeton signé + vérification BD** | Contrôle instantané en base de l'authenticité de la carte imprimée et de l'état libre du baptisé. |

---

## 8. Points à valider avant la suite

Merci de trancher ces points pour passer à la conception détaillée (base de données, maquettes et architecture du code) :

1. **Nom officiel** : Confirmez-vous `SIGEB` (ou un autre nom) ?
2. **Accès Fidèles** : Souhaitez-vous l'inclure dès la V1 ou démarrer uniquement avec le clergé et les secrétariats paroissiaux ?
3. **Bilinguisme FR / EN** : Intégré dès la V1 ?
4. **Mode hors-ligne** : Critique dès la V1 (zones rurales) ou reporté en V2 ?
5. **Paiement Mobile Money (MoMo / OM)** : Inclus dans le périmètre ou géré hors application dans un premier temps ?
6. **Format de la carte** : Disposez-vous déjà d'un modèle officiel (papier) de la carte de baptême au Cameroun à reproduire fidèlement ?

-- =====================================================================
-- SIGEB — V1 : schéma initial (14 tables, index, RLS, rôles, audit chaîné)
-- Référence : MCD_SIGEB.md (v2)
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------
-- Rôles applicatifs (créés ici s'ils n'existent pas ; en dev ils sont
-- créés avec mot de passe par infra/db/init/01-roles.sql)
-- ---------------------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sigeb_app') THEN
    CREATE ROLE sigeb_app NOLOGIN;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sigeb_readonly') THEN
    CREATE ROLE sigeb_readonly NOLOGIN;
  END IF;
END $$;

-- ---------------------------------------------------------------------
-- A. Référentiel ecclésial & utilisateurs
-- ---------------------------------------------------------------------
CREATE TABLE province_ecclesiastique (
  id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code    VARCHAR(10)  NOT NULL UNIQUE,
  nom_fr  VARCHAR(120) NOT NULL,
  nom_en  VARCHAR(120) NOT NULL
);

CREATE TABLE diocese (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  province_id   UUID         NOT NULL REFERENCES province_ecclesiastique(id),
  code          VARCHAR(10)  NOT NULL UNIQUE,
  type_diocese  VARCHAR(20)  NOT NULL CHECK (type_diocese IN ('DIOCESE','ARCHIDIOCESE')),
  nom_fr        VARCHAR(150) NOT NULL,
  nom_en        VARCHAR(150) NOT NULL,
  siege         VARCHAR(100) NOT NULL,
  actif         BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE paroisse (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  diocese_id       UUID         NOT NULL REFERENCES diocese(id),
  code             VARCHAR(10)  NOT NULL,
  type_paroisse    VARCHAR(20)  NOT NULL CHECK (type_paroisse IN ('PAROISSE','QUASI_PAROISSE','MISSION')),
  nom_fr           VARCHAR(160) NOT NULL,
  nom_en           VARCHAR(160) NOT NULL,
  localite         VARCHAR(120) NOT NULL,
  entete_officiel  VARCHAR(255),
  actif            BOOLEAN      NOT NULL DEFAULT TRUE,
  UNIQUE (diocese_id, code)
);

CREATE TABLE utilisateur (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  identifiant          VARCHAR(120) NOT NULL UNIQUE,
  mot_de_passe_hash    VARCHAR(255) NOT NULL,
  pin_signature_hash   VARCHAR(255),
  totp_secret_chiffre  BYTEA,
  totp_active          BOOLEAN      NOT NULL DEFAULT FALSE,
  mdp_a_changer        BOOLEAN      NOT NULL DEFAULT TRUE,
  nom_complet          VARCHAR(160) NOT NULL,
  telephone            VARCHAR(30),
  role                 VARCHAR(25)  NOT NULL
                       CHECK (role IN ('ADMIN_NATIONAL','CHANCELIER','CURE','SECRETAIRE')),
  diocese_id           UUID REFERENCES diocese(id),
  paroisse_id          UUID REFERENCES paroisse(id),
  langue_preferee      VARCHAR(2)   NOT NULL DEFAULT 'FR' CHECK (langue_preferee IN ('FR','EN')),
  statut               VARCHAR(20)  NOT NULL DEFAULT 'ACTIF' CHECK (statut IN ('ACTIF','SUSPENDU')),
  tentatives_echec     SMALLINT     NOT NULL DEFAULT 0,
  verrouille_jusqua    TIMESTAMPTZ,
  cree_le              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT ck_utilisateur_perimetre CHECK (
    (role = 'ADMIN_NATIONAL')
    OR (role = 'CHANCELIER' AND diocese_id IS NOT NULL)
    OR (role IN ('CURE','SECRETAIRE') AND diocese_id IS NOT NULL AND paroisse_id IS NOT NULL)
  )
);

CREATE TABLE affectation_utilisateur (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  utilisateur_id UUID        NOT NULL REFERENCES utilisateur(id),
  role           VARCHAR(25) NOT NULL,
  diocese_id     UUID REFERENCES diocese(id),
  paroisse_id    UUID REFERENCES paroisse(id),
  date_debut     DATE        NOT NULL,
  date_fin       DATE,
  CHECK (date_fin IS NULL OR date_fin >= date_debut)
);
CREATE INDEX ix_affectation_utilisateur ON affectation_utilisateur (utilisateur_id, date_fin);

-- ---------------------------------------------------------------------
-- B. Actes, avenants, mentions marginales
-- ---------------------------------------------------------------------
CREATE TABLE acte_bapteme (
  id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  iun                       VARCHAR(40),
  paroisse_id               UUID         NOT NULL REFERENCES paroisse(id),
  origine                   VARCHAR(20)  NOT NULL CHECK (origine IN ('NOUVEAU','REPRISE_ARCHIVE')),
  statut                    VARCHAR(25)  NOT NULL
                            CHECK (statut IN ('BROUILLON','EN_ATTENTE_VALIDATION','A_CORRIGER','SCELLE')),
  version                   INTEGER      NOT NULL DEFAULT 1,
  nom                       VARCHAR(120) NOT NULL,
  prenoms                   VARCHAR(120) NOT NULL,
  nom_bapteme               VARCHAR(120),
  sexe                      CHAR(1)      NOT NULL CHECK (sexe IN ('M','F')),
  date_naissance            DATE         NOT NULL,
  lieu_naissance            VARCHAR(150) NOT NULL,
  nom_pere                  VARCHAR(160),
  nom_mere                  VARCHAR(160),
  filiation_confidentielle  BOOLEAN      NOT NULL DEFAULT FALSE,
  nom_parrain               VARCHAR(160),
  nom_marraine              VARCHAR(160),
  paroisse_parrainage       VARCHAR(160),
  date_bapteme              DATE         NOT NULL,
  lieu_bapteme              VARCHAR(160) NOT NULL,
  ministre_celebrant        VARCHAR(160) NOT NULL,
  registre_tome             VARCHAR(20)  NOT NULL,
  registre_page             VARCHAR(20)  NOT NULL,
  registre_numero           VARCHAR(30)  NOT NULL,
  motif_correction          TEXT,
  saisi_par_id              UUID         NOT NULL REFERENCES utilisateur(id),
  scelle_par_id             UUID REFERENCES utilisateur(id),
  scelle_le                 TIMESTAMPTZ,
  empreinte_scellement      VARCHAR(64),
  cree_le                   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  modifie_le                TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT ck_acte_dates CHECK (date_bapteme >= date_naissance),
  CONSTRAINT ck_acte_parrainage CHECK (nom_parrain IS NOT NULL OR nom_marraine IS NOT NULL),
  CONSTRAINT ck_acte_scelle CHECK (
    statut <> 'SCELLE'
    OR (iun IS NOT NULL AND scelle_par_id IS NOT NULL AND scelle_le IS NOT NULL AND empreinte_scellement IS NOT NULL)
  )
);
CREATE UNIQUE INDEX ux_acte_iun ON acte_bapteme (iun) WHERE iun IS NOT NULL;
CREATE UNIQUE INDEX ux_acte_registre ON acte_bapteme (paroisse_id, registre_tome, registre_page, registre_numero);
CREATE INDEX ix_acte_identite ON acte_bapteme (LOWER(nom), LOWER(prenoms), date_naissance);
CREATE INDEX ix_acte_paroisse_statut ON acte_bapteme (paroisse_id, statut);

CREATE TABLE avenant_rectification (
  id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  acte_bapteme_id         UUID        NOT NULL REFERENCES acte_bapteme(id),
  version_cible           INTEGER     NOT NULL,
  gravite                 VARCHAR(15) NOT NULL CHECK (gravite IN ('MINEURE','MAJEURE')),
  anciennes_valeurs_json  JSONB       NOT NULL,
  nouvelles_valeurs_json  JSONB       NOT NULL,
  motif                   TEXT        NOT NULL,
  reference_justificatif  VARCHAR(180),
  statut                  VARCHAR(25) NOT NULL
                          CHECK (statut IN ('EN_ATTENTE_CHANCELIER','APPROUVE','REJETE')),
  demande_par_id          UUID        NOT NULL REFERENCES utilisateur(id),
  valide_par_id           UUID REFERENCES utilisateur(id),
  demande_le              TIMESTAMPTZ NOT NULL DEFAULT now(),
  valide_le               TIMESTAMPTZ,
  UNIQUE (acte_bapteme_id, version_cible)
);

CREATE TABLE mention_marginale (
  id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  acte_bapteme_id         UUID        NOT NULL REFERENCES acte_bapteme(id),
  paroisse_declarante_id  UUID        NOT NULL REFERENCES paroisse(id),
  type_sacrement          VARCHAR(30) NOT NULL
                          CHECK (type_sacrement IN ('CONFIRMATION','MARIAGE','NULLITE_MARIAGE',
                                 'ORDINATION_DIACONAT','ORDINATION_PRESBYTERAT','VOEUX_PERPETUELS')),
  date_evenement          DATE         NOT NULL,
  lieu_evenement          VARCHAR(160) NOT NULL,
  ministre_celebrant      VARCHAR(160) NOT NULL,
  conjoint_nom_complet    VARCHAR(180),
  ref_registre_local      VARCHAR(80),
  statut                  VARCHAR(25)  NOT NULL
                          CHECK (statut IN ('EN_ATTENTE_ORIGINE','INSCRITE','REJETEE')),
  declare_par_id          UUID         NOT NULL REFERENCES utilisateur(id),
  valide_par_id           UUID REFERENCES utilisateur(id),
  declare_le              TIMESTAMPTZ  NOT NULL DEFAULT now(),
  valide_le               TIMESTAMPTZ,
  motif_rejet             TEXT,
  CONSTRAINT ck_mention_rejet CHECK (statut <> 'REJETEE' OR motif_rejet IS NOT NULL)
);
CREATE INDEX ix_mention_acte ON mention_marginale (acte_bapteme_id, statut);

-- ---------------------------------------------------------------------
-- E. Clés de signature QR (créée avant carte_emise)
-- ---------------------------------------------------------------------
CREATE TABLE cle_signature (
  kid             VARCHAR(16) PRIMARY KEY,
  secret_chiffre  BYTEA       NOT NULL,
  statut          VARCHAR(15) NOT NULL CHECK (statut IN ('ACTIVE','VERIFY_ONLY','RETIREE')),
  cree_le         TIMESTAMPTZ NOT NULL DEFAULT now(),
  retiree_le      TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_cle_signature_active ON cle_signature (statut) WHERE statut = 'ACTIVE';

-- ---------------------------------------------------------------------
-- C. Cartes émises
-- ---------------------------------------------------------------------
CREATE TABLE carte_emise (
  id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  acte_bapteme_id         UUID        NOT NULL REFERENCES acte_bapteme(id),
  paroisse_emettrice_id   UUID        NOT NULL REFERENCES paroisse(id),
  code_carte              VARCHAR(30) NOT NULL UNIQUE,
  format_document         VARCHAR(20) NOT NULL CHECK (format_document IN ('CARTE','EXTRAIT_A4')),
  langue                  VARCHAR(2)  NOT NULL CHECK (langue IN ('FR','EN')),
  type_emission           VARCHAR(15) NOT NULL CHECK (type_emission IN ('ORIGINALE','DUPLICATA')),
  numero_duplicata        SMALLINT    NOT NULL DEFAULT 0,
  motif_emission          VARCHAR(30) NOT NULL
                          CHECK (motif_emission IN ('PREMIERE_EMISSION','PERTE','VOL','DETERIORATION',
                                 'MISE_A_JOUR','MARIAGE','CONFIRMATION','SEMINAIRE')),
  version_acte_au_tirage  INTEGER     NOT NULL,
  nb_mentions_au_tirage   SMALLINT    NOT NULL,
  donnees_imprimees_json  JSONB       NOT NULL,
  empreinte_donnees       VARCHAR(64) NOT NULL,
  template_version        VARCHAR(10) NOT NULL,
  qr_token                VARCHAR(64) NOT NULL UNIQUE,
  qr_kid                  VARCHAR(16) NOT NULL REFERENCES cle_signature(kid),
  qr_signature            VARCHAR(128) NOT NULL,
  statut                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                          CHECK (statut IN ('ACTIVE','OBSOLETE','REMPLACEE','REVOQUEE')),
  remplacee_par_carte_id  UUID REFERENCES carte_emise(id),
  revoquee_le             TIMESTAMPTZ,
  motif_revocation        VARCHAR(180),
  emis_par_id             UUID        NOT NULL REFERENCES utilisateur(id),
  emis_le                 TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_carte_duplicata CHECK (
    (type_emission = 'ORIGINALE' AND numero_duplicata = 0)
    OR (type_emission = 'DUPLICATA' AND numero_duplicata >= 1)
  ),
  UNIQUE (acte_bapteme_id, format_document, numero_duplicata)
);
-- Une seule carte ACTIVE par acte et par format
CREATE UNIQUE INDEX ux_carte_active ON carte_emise (acte_bapteme_id, format_document) WHERE statut = 'ACTIVE';

-- ---------------------------------------------------------------------
-- D. Audit chaîné & compteur IUN
-- ---------------------------------------------------------------------
CREATE TABLE journal_audit (
  id               BIGSERIAL PRIMARY KEY,
  horodatage       TIMESTAMPTZ NOT NULL DEFAULT now(),
  utilisateur_id   UUID REFERENCES utilisateur(id),
  role_utilisateur VARCHAR(25) NOT NULL,
  diocese_id       UUID,
  paroisse_id      UUID,
  type_action      VARCHAR(30) NOT NULL
                   CHECK (type_action IN ('CONNEXION','CREATION_ACTE','SCELLEMENT','RECTIFICATION',
                          'EMISSION_CARTE','DUPLICATA','REVOCATION_CARTE','SCAN_QR',
                          'RECHERCHE_NATIONALE','MENTION_DECLAREE','MENTION_VALIDEE')),
  entite_cible     VARCHAR(40),
  entite_id        UUID,
  iun_concerne     VARCHAR(40),
  motif            TEXT,
  details_json     JSONB,
  adresse_ip       VARCHAR(64),
  hash_precedent   CHAR(64) NOT NULL,
  hash_ligne       CHAR(64) NOT NULL,
  CONSTRAINT ck_audit_motif CHECK (
    type_action NOT IN ('RECHERCHE_NATIONALE','DUPLICATA','RECTIFICATION') OR motif IS NOT NULL
  )
);
CREATE INDEX ix_audit_perimetre ON journal_audit (horodatage DESC, diocese_id, paroisse_id);
CREATE INDEX ix_audit_iun ON journal_audit (iun_concerne);

CREATE TABLE compteur_iun (
  paroisse_id    UUID     NOT NULL REFERENCES paroisse(id),
  annee          SMALLINT NOT NULL,
  dernier_numero INTEGER  NOT NULL DEFAULT 0,
  PRIMARY KEY (paroisse_id, annee)
);

-- Chaînage de hash calculé par la base (indépendant du code applicatif)
CREATE OR REPLACE FUNCTION audit_chainer() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
  prev CHAR(64);
BEGIN
  PERFORM pg_advisory_xact_lock(918273645);  -- sérialise l'écriture de la chaîne
  SELECT hash_ligne INTO prev FROM journal_audit ORDER BY id DESC LIMIT 1;
  NEW.hash_precedent := COALESCE(prev, repeat('0', 64));
  NEW.hash_ligne := encode(digest(concat_ws('|',
      NEW.id, NEW.horodatage, NEW.utilisateur_id, NEW.role_utilisateur, NEW.diocese_id,
      NEW.paroisse_id, NEW.type_action, NEW.entite_cible, NEW.entite_id, NEW.iun_concerne,
      NEW.motif, NEW.details_json, NEW.adresse_ip, NEW.hash_precedent), 'sha256'), 'hex');
  RETURN NEW;
END $$;

CREATE TRIGGER trg_audit_chainer BEFORE INSERT ON journal_audit
  FOR EACH ROW EXECUTE FUNCTION audit_chainer();

-- ---------------------------------------------------------------------
-- Sécurité applicative : refresh tokens & idempotence
-- ---------------------------------------------------------------------
CREATE TABLE refresh_token (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  utilisateur_id UUID        NOT NULL REFERENCES utilisateur(id),
  token_hash     VARCHAR(64) NOT NULL UNIQUE,
  famille_id     UUID        NOT NULL,
  expire_le      TIMESTAMPTZ NOT NULL,
  revoque_le     TIMESTAMPTZ
);
CREATE INDEX ix_refresh_famille ON refresh_token (utilisateur_id, famille_id);

CREATE TABLE requete_idempotente (
  utilisateur_id    UUID        NOT NULL REFERENCES utilisateur(id),
  cle               UUID        NOT NULL,
  empreinte_requete CHAR(64)    NOT NULL,
  statut_http       SMALLINT,
  reponse_json      JSONB,
  cree_le           TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (utilisateur_id, cle)
);
CREATE INDEX ix_idempotente_cree ON requete_idempotente (cree_le);

-- ---------------------------------------------------------------------
-- Row-Level Security : fonctions de contexte
-- ---------------------------------------------------------------------
CREATE FUNCTION app_role() RETURNS text LANGUAGE sql STABLE AS
  $$ SELECT NULLIF(current_setting('app.role', true), '') $$;
CREATE FUNCTION app_paroisse() RETURNS uuid LANGUAGE sql STABLE AS
  $$ SELECT NULLIF(current_setting('app.paroisse_id', true), '')::uuid $$;
CREATE FUNCTION app_diocese() RETURNS uuid LANGUAGE sql STABLE AS
  $$ SELECT NULLIF(current_setting('app.diocese_id', true), '')::uuid $$;

-- Fonctions SECURITY DEFINER : lisent hors RLS pour résoudre l'appartenance
CREATE FUNCTION paroisse_dans_diocese(p uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS
  $$ SELECT EXISTS (SELECT 1 FROM paroisse WHERE id = p AND diocese_id = app_diocese()) $$;

CREATE FUNCTION acte_paroisse(a uuid) RETURNS uuid
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS
  $$ SELECT paroisse_id FROM acte_bapteme WHERE id = a $$;

-- Prédicat commun : le périmètre de l'utilisateur couvre-t-il cette paroisse ?
CREATE FUNCTION perimetre_couvre(p uuid) RETURNS boolean
LANGUAGE sql STABLE AS $$
  SELECT CASE
    WHEN app_role() = 'ADMIN_NATIONAL' THEN TRUE
    WHEN app_role() = 'CHANCELIER'     THEN paroisse_dans_diocese(p)
    WHEN app_role() IN ('CURE','SECRETAIRE') THEN p = app_paroisse()
    ELSE FALSE
  END
$$;

ALTER TABLE acte_bapteme          ENABLE ROW LEVEL SECURITY;
ALTER TABLE avenant_rectification ENABLE ROW LEVEL SECURITY;
ALTER TABLE mention_marginale     ENABLE ROW LEVEL SECURITY;
ALTER TABLE carte_emise           ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_audit         ENABLE ROW LEVEL SECURITY;

CREATE POLICY cloisonnement ON acte_bapteme
  USING (perimetre_couvre(paroisse_id));

CREATE POLICY cloisonnement ON avenant_rectification
  USING (perimetre_couvre(acte_paroisse(acte_bapteme_id)));

CREATE POLICY cloisonnement ON mention_marginale
  USING (perimetre_couvre(paroisse_declarante_id)
         OR perimetre_couvre(acte_paroisse(acte_bapteme_id)));

CREATE POLICY cloisonnement ON carte_emise
  USING (perimetre_couvre(paroisse_emettrice_id)
         OR perimetre_couvre(acte_paroisse(acte_bapteme_id)));

-- Audit : lecture limitée au périmètre, écriture libre (append-only)
CREATE POLICY audit_lecture ON journal_audit FOR SELECT
  USING (
    app_role() = 'ADMIN_NATIONAL'
    OR (app_role() = 'CHANCELIER' AND diocese_id = app_diocese())
    OR (app_role() = 'CURE' AND paroisse_id = app_paroisse())
  );
CREATE POLICY audit_ecriture ON journal_audit FOR INSERT WITH CHECK (TRUE);

-- ---------------------------------------------------------------------
-- Droits
-- ---------------------------------------------------------------------
GRANT USAGE ON SCHEMA public TO sigeb_app, sigeb_readonly;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO sigeb_app;
GRANT DELETE ON refresh_token, requete_idempotente TO sigeb_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO sigeb_app;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO sigeb_readonly;

-- Audit append-only : aucune modification possible par l'application
REVOKE UPDATE, DELETE, TRUNCATE ON journal_audit FROM sigeb_app, sigeb_readonly, PUBLIC;

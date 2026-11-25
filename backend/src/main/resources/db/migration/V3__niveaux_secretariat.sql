-- =====================================================================
-- SIGEB — V3 : Séparation Niveau Ecclésial (NATIONAL/PROVINCE/DIOCESE/PAROISSE)
--              et Rôle (autorité), support des secrétariats à tous les niveaux,
--              support CATHEDRALE et RLS provinciale.
-- =====================================================================

-- 1. Ajout de CATHEDRALE dans paroisse.type_paroisse
ALTER TABLE paroisse DROP CONSTRAINT IF EXISTS paroisse_type_paroisse_check;
ALTER TABLE paroisse ADD CONSTRAINT paroisse_type_paroisse_check
  CHECK (type_paroisse IN ('PAROISSE','CATHEDRALE','QUASI_PAROISSE','MISSION'));

-- 2. Colonnes niveau & province_id sur utilisateur
ALTER TABLE utilisateur
  ADD COLUMN niveau VARCHAR(15),
  ADD COLUMN province_id UUID REFERENCES province_ecclesiastique(id);

-- Rétro-remplissage si des lignes existent déjà
UPDATE utilisateur u
SET province_id = d.province_id
FROM diocese d
WHERE u.diocese_id = d.id AND u.province_id IS NULL;

UPDATE utilisateur
SET niveau = CASE
  WHEN role = 'ADMIN_NATIONAL' THEN 'NATIONAL'
  WHEN role = 'CHANCELIER'     THEN 'DIOCESE'
  ELSE 'PAROISSE'
END
WHERE niveau IS NULL;

ALTER TABLE utilisateur
  ALTER COLUMN niveau SET NOT NULL,
  ALTER COLUMN niveau SET DEFAULT 'PAROISSE';

ALTER TABLE utilisateur DROP CONSTRAINT IF EXISTS utilisateur_role_check;
ALTER TABLE utilisateur ADD CONSTRAINT utilisateur_role_check
  CHECK (role IN ('ADMIN_NATIONAL','RESPONSABLE_PROVINCE','CHANCELIER','CURE','SECRETAIRE'));

ALTER TABLE utilisateur ADD CONSTRAINT ck_utilisateur_niveau
  CHECK (niveau IN ('NATIONAL','PROVINCE','DIOCESE','PAROISSE'));

ALTER TABLE utilisateur DROP CONSTRAINT IF EXISTS ck_utilisateur_perimetre;
ALTER TABLE utilisateur ADD CONSTRAINT ck_utilisateur_perimetre CHECK (
  (niveau = 'NATIONAL' AND province_id IS NULL     AND diocese_id IS NULL     AND paroisse_id IS NULL)
  OR (niveau = 'PROVINCE' AND province_id IS NOT NULL AND diocese_id IS NULL     AND paroisse_id IS NULL)
  OR (niveau = 'DIOCESE'  AND province_id IS NOT NULL AND diocese_id IS NOT NULL AND paroisse_id IS NULL)
  OR (niveau = 'PAROISSE' AND province_id IS NOT NULL AND diocese_id IS NOT NULL AND paroisse_id IS NOT NULL)
);

ALTER TABLE utilisateur ADD CONSTRAINT ck_utilisateur_role_niveau CHECK (
  (role = 'ADMIN_NATIONAL'       AND niveau = 'NATIONAL')
  OR (role = 'RESPONSABLE_PROVINCE' AND niveau = 'PROVINCE')
  OR (role = 'CHANCELIER'        AND niveau = 'DIOCESE')
  OR (role = 'CURE'              AND niveau = 'PAROISSE')
  OR (role = 'SECRETAIRE')
);

-- 3. Colonnes niveau & province_id sur affectation_utilisateur
ALTER TABLE affectation_utilisateur
  ADD COLUMN niveau VARCHAR(15) NOT NULL DEFAULT 'PAROISSE'
    CHECK (niveau IN ('NATIONAL','PROVINCE','DIOCESE','PAROISSE')),
  ADD COLUMN province_id UUID REFERENCES province_ecclesiastique(id);

-- 4. Colonne province_id sur journal_audit pour le filtrage d'audit provincial
ALTER TABLE journal_audit
  ADD COLUMN province_id UUID;

-- 5. Mise à jour des fonctions et politiques Row-Level Security (support PROVINCE + niveau)
CREATE OR REPLACE FUNCTION app_niveau() RETURNS text LANGUAGE sql STABLE AS
  $$ SELECT NULLIF(current_setting('app.niveau', true), '') $$;

CREATE OR REPLACE FUNCTION app_province() RETURNS uuid LANGUAGE sql STABLE AS
  $$ SELECT NULLIF(current_setting('app.province_id', true), '')::uuid $$;

CREATE OR REPLACE FUNCTION paroisse_dans_province(p uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (
    SELECT 1 FROM paroisse pa
    JOIN diocese d ON d.id = pa.diocese_id
    WHERE pa.id = p AND d.province_id = app_province()
  )
$$;

CREATE OR REPLACE FUNCTION perimetre_couvre(p uuid) RETURNS boolean
LANGUAGE sql STABLE AS $$
  SELECT CASE
    WHEN app_niveau() = 'NATIONAL' OR app_role() = 'ADMIN_NATIONAL' THEN TRUE
    WHEN app_niveau() = 'PROVINCE' OR app_role() = 'RESPONSABLE_PROVINCE' THEN paroisse_dans_province(p)
    WHEN app_niveau() = 'DIOCESE'  OR app_role() = 'CHANCELIER' THEN paroisse_dans_diocese(p)
    WHEN app_niveau() = 'PAROISSE' OR app_role() IN ('CURE','SECRETAIRE') THEN p = app_paroisse()
    ELSE FALSE
  END
$$;

DROP POLICY IF EXISTS audit_lecture ON journal_audit;
CREATE POLICY audit_lecture ON journal_audit FOR SELECT
  USING (
    app_niveau() = 'NATIONAL' OR app_role() = 'ADMIN_NATIONAL'
    OR ((app_niveau() = 'PROVINCE' OR app_role() = 'RESPONSABLE_PROVINCE') AND province_id = app_province())
    OR ((app_niveau() = 'DIOCESE'  OR app_role() = 'CHANCELIER') AND diocese_id = app_diocese())
    OR (app_role() = 'CURE' AND paroisse_id = app_paroisse())
  );

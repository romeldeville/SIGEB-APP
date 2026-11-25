-- Les 5 provinces ecclésiastiques du Cameroun.
-- Les diocèses seront ajoutés après validation de la liste par la CENC.
-- Le Super Admin initial est créé au démarrage par l'application (BootstrapAdminRunner)
-- à partir de variables d'environnement, afin de ne jamais stocker de hash dans Git.

INSERT INTO province_ecclesiastique (code, nom_fr, nom_en) VALUES
  ('YDE', 'Yaoundé', 'Yaounde'),
  ('DLA', 'Douala',  'Douala'),
  ('BMD', 'Bamenda', 'Bamenda'),
  ('GRA', 'Garoua',  'Garoua'),
  ('BTA', 'Bertoua', 'Bertoua');

-- Rôles de DEV avec mot de passe (en prod : mots de passe via secrets)
CREATE ROLE sigeb_app LOGIN PASSWORD 'sigeb_app_dev';
CREATE ROLE sigeb_readonly LOGIN PASSWORD 'sigeb_readonly_dev';

#!/usr/bin/env bash
# Sauvegarde quotidienne chiffrée PostgreSQL (SIGEB — 100 % BD)
set -euo pipefail

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_DIR:-/var/backups/sigeb}"
mkdir -p "$BACKUP_DIR"

DUMP_FILE="$BACKUP_DIR/sigeb_${DATE}.sql.gz"
ENC_FILE="${DUMP_FILE}.age"

docker exec sigeb-db pg_dump -U "${FLYWAY_USER:-sigeb_owner}" -Fc sigeb | gzip -9 > "$DUMP_FILE"

if [ -n "${BACKUP_AGE_PUBLIC_KEY:-}" ]; then
  age -r "$BACKUP_AGE_PUBLIC_KEY" -o "$ENC_FILE" "$DUMP_FILE"
  rm -f "$DUMP_FILE"
  echo "Backup chiffré créé : $ENC_FILE"
else
  echo "ATTENTION : BACKUP_AGE_PUBLIC_KEY non définie, dump non chiffré : $DUMP_FILE"
fi

# Rétention locale : 30 jours (copie hors-serveur via rclone/rsync recommandée)
find "$BACKUP_DIR" -type f -mtime +30 -delete

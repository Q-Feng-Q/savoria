#!/usr/bin/env bash
set -euo pipefail

MYSQL_DATABASE=${MYSQL_DATABASE:-family_kitchen}
MYSQL_SOCKET=${MYSQL_SOCKET:-/run/mysqld/mysqld.sock}
MYSQL_ROOT_CLIENT_CONFIG=${MYSQL_ROOT_CLIENT_CONFIG:-/run/mysql-root-client.cnf}
BACKUP_ROOT=${BACKUP_ROOT:-/data/backups}
timestamp=$(date '+%Y%m%d-%H%M%S')
database_backup="$BACKUP_ROOT/family-kitchen-db-$timestamp.sql.gz"
files_backup="$BACKUP_ROOT/family-kitchen-files-$timestamp.tar.gz"

mkdir -p "$BACKUP_ROOT"

mysqldump \
  --defaults-extra-file="$MYSQL_ROOT_CLIENT_CONFIG" \
  --protocol=socket \
  --socket="$MYSQL_SOCKET" \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  "$MYSQL_DATABASE" | gzip -9 > "$database_backup"

tar -czf "$files_backup" \
  -C /data \
  uploads \
  feedback-private \
  dish-template-assets

printf '数据库备份：%s\n' "$database_backup"
printf '文件备份：%s\n' "$files_backup"

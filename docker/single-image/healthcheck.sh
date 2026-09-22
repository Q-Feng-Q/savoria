#!/usr/bin/env bash
set -eu

MYSQL_SOCKET=${MYSQL_SOCKET:-/run/mysqld/mysqld.sock}
MYSQL_ROOT_CLIENT_CONFIG=${MYSQL_ROOT_CLIENT_CONFIG:-/run/mysql-root-client.cnf}

mysqladmin --defaults-extra-file="$MYSQL_ROOT_CLIENT_CONFIG" \
  --protocol=socket --socket="$MYSQL_SOCKET" ping --silent >/dev/null
curl --fail --silent --show-error --max-time 5 \
  http://127.0.0.1:8080/api/public/system-settings >/dev/null

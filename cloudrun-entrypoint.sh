#!/bin/sh
set -eu

fail() {
  printf '[cloudrun-entrypoint] %s\n' "$1" >&2
  exit 78
}

required_variables='MYSQL_ADDRESS MYSQL_DATABASE MYSQL_USERNAME MYSQL_PASSWORD FAMILY_KITCHEN_JWT_SECRET FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD FAMILY_KITCHEN_WECHAT_APP_ID FAMILY_KITCHEN_WECHAT_APP_SECRET'

for variable_name in $required_variables; do
  eval "variable_value=\${$variable_name-}"
  [ -n "$variable_value" ] || fail "missing required environment variable: $variable_name"

  case "$variable_value" in
    123456|请替换*|ReplaceWith*)
      fail "development placeholder is not allowed: $variable_name"
      ;;
  esac
done

case "$MYSQL_ADDRESS" in
  localhost|localhost:*|127.0.0.1|127.0.0.1:*)
    fail 'MYSQL_ADDRESS must point to CloudBase MySQL or another reachable database, not this container.'
    ;;
  *:*) ;;
  *) fail 'MYSQL_ADDRESS must use host:port format.' ;;
esac

case "${PORT:-8080}" in
  ''|*[!0-9]*) fail 'PORT must be numeric.' ;;
esac

[ "${#FAMILY_KITCHEN_JWT_SECRET}" -ge 32 ] \
  || fail 'FAMILY_KITCHEN_JWT_SECRET must contain at least 32 characters.'
[ "${#FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD}" -ge 6 ] \
  || fail 'FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD must contain at least 6 characters.'

printf '[cloudrun-entrypoint] database=%s/%s user=%s port=%s\n' \
  "$MYSQL_ADDRESS" "$MYSQL_DATABASE" "$MYSQL_USERNAME" "${PORT:-8080}"
printf '[cloudrun-entrypoint] starting Spring Boot\n'

exec java -jar /app/app.jar

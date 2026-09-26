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
case "${SERVER_PORT:-8081}" in
  ''|*[!0-9]*) fail 'SERVER_PORT must be numeric.' ;;
esac
[ "${PORT:-8080}" != "${SERVER_PORT:-8081}" ] \
  || fail 'PORT and SERVER_PORT must use different ports.'
[ "${SERVER_ADDRESS:-0.0.0.0}" = '0.0.0.0' ] \
  || fail 'SERVER_ADDRESS must be 0.0.0.0 in WeChat Cloud Hosting.'

[ "${#FAMILY_KITCHEN_JWT_SECRET}" -ge 32 ] \
  || fail 'FAMILY_KITCHEN_JWT_SECRET must contain at least 32 characters.'
[ "${#FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD}" -ge 6 ] \
  || fail 'FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD must contain at least 6 characters.'

printf '[cloudrun-entrypoint] database=%s/%s user=%s gateway=%s app=%s:%s\n' \
  "$MYSQL_ADDRESS" "$MYSQL_DATABASE" "$MYSQL_USERNAME" "${PORT:-8080}" \
  "${SERVER_ADDRESS:-0.0.0.0}" "${SERVER_PORT:-8081}"

java_pid=''
nginx_pid=''

stop_children() {
  [ -n "$nginx_pid" ] && kill -TERM "$nginx_pid" 2>/dev/null || true
  [ -n "$java_pid" ] && kill -TERM "$java_pid" 2>/dev/null || true
}

wait_for_children() {
  set +e
  [ -n "$nginx_pid" ] && wait "$nginx_pid" 2>/dev/null
  [ -n "$java_pid" ] && wait "$java_pid" 2>/dev/null
  set -e
}

shutdown() {
  stop_children
  wait_for_children
  exit 0
}

trap shutdown TERM INT

printf '[cloudrun-entrypoint] starting Spring Boot on %s:%s\n' \
  "${SERVER_ADDRESS:-0.0.0.0}" "${SERVER_PORT:-8081}"
java -jar /app/app.jar &
java_pid=$!

printf '[cloudrun-entrypoint] starting Nginx on 0.0.0.0:%s\n' "${PORT:-8080}"
nginx -g 'daemon off;' &
nginx_pid=$!

while true; do
  if ! kill -0 "$java_pid" 2>/dev/null; then
    printf '[cloudrun-entrypoint] Java process exited unexpectedly\n' >&2
    java_status=0
    wait "$java_pid" || java_status=$?
    stop_children
    wait_for_children
    exit "$java_status"
  fi

  if ! kill -0 "$nginx_pid" 2>/dev/null; then
    printf '[cloudrun-entrypoint] Nginx process exited unexpectedly\n' >&2
    nginx_status=0
    wait "$nginx_pid" || nginx_status=$?
    stop_children
    wait_for_children
    exit "$nginx_status"
  fi

  sleep 1
done

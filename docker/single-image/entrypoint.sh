#!/bin/sh
set -eu

required_variables="
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
FAMILY_KITCHEN_JWT_SECRET
FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD
"

for variable_name in $required_variables; do
  eval "variable_value=\${$variable_name-}"
  if [ -z "$variable_value" ]; then
    echo "[entrypoint] required environment variable is empty: $variable_name" >&2
    exit 1
  fi

  case "$variable_value" in
    123456|family-kitchen-local-jwt-secret-change-before-production-2026|请替换*)
      echo "[entrypoint] development placeholder is not allowed: $variable_name" >&2
      exit 1
      ;;
  esac
done

java_pid=
nginx_pid=
shutting_down=0

stop_children() {
  signal_name=${1:-TERM}
  [ -n "$java_pid" ] && kill -"$signal_name" "$java_pid" 2>/dev/null || true
  [ -n "$nginx_pid" ] && kill -"$signal_name" "$nginx_pid" 2>/dev/null || true
}

wait_for_children() {
  set +e
  [ -n "$java_pid" ] && wait "$java_pid" 2>/dev/null
  [ -n "$nginx_pid" ] && wait "$nginx_pid" 2>/dev/null
  set -e
}

handle_signal() {
  signal_name=$1
  shutting_down=1
  stop_children "$signal_name"
  if [ "$signal_name" = "QUIT" ]; then
    (
      sleep 5
      [ -n "$java_pid" ] && kill -TERM "$java_pid" 2>/dev/null || true
      [ -n "$nginx_pid" ] && kill -TERM "$nginx_pid" 2>/dev/null || true
    ) &
  fi
  wait_for_children
  exit 0
}

trap 'handle_signal TERM' TERM
trap 'handle_signal INT' INT
trap 'handle_signal QUIT' QUIT

# JAVA_OPTS intentionally supports the conventional whitespace-separated JVM option form.
# shellcheck disable=SC2086
java ${JAVA_OPTS:-} -jar /app/app.jar &
java_pid=$!

nginx -g 'daemon off;' &
nginx_pid=$!

while [ "$shutting_down" -eq 0 ]; do
  if ! kill -0 "$java_pid" 2>/dev/null; then
    echo "[entrypoint] Java process exited unexpectedly" >&2
    stop_children
    wait_for_children
    exit 1
  fi

  if ! kill -0 "$nginx_pid" 2>/dev/null; then
    echo "[entrypoint] Nginx process exited unexpectedly" >&2
    stop_children
    wait_for_children
    exit 1
  fi

  sleep 1
done

exit 1

#!/usr/bin/env bash
set -eu

required_variables="
MYSQL_APP_PASSWORD
FAMILY_KITCHEN_JWT_SECRET
FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD
"

fail() {
  printf '[entrypoint] %s\n' "$1" >&2
  exit "${2:-1}"
}

for variable_name in $required_variables; do
  eval "variable_value=\${$variable_name-}"
  if [ -z "$variable_value" ]; then
    fail "required environment variable is empty: $variable_name"
  fi

  case "$variable_value" in
    123456|family-kitchen-local-jwt-secret-change-before-production-2026|请替换*|ReplaceWith*)
      fail "development placeholder is not allowed: $variable_name"
      ;;
  esac
done

MYSQL_DATABASE=${MYSQL_DATABASE:-family_kitchen}
MYSQL_APP_USERNAME=${MYSQL_APP_USERNAME:-family_kitchen}
MYSQL_DATADIR=${MYSQL_DATADIR:-/data/mysql}
MYSQL_SOCKET=${MYSQL_SOCKET:-/run/mysqld/mysqld.sock}
MYSQL_ROOT_PASSWORD_FILE=${MYSQL_ROOT_PASSWORD_FILE:-/data/config/mysql-root-password}
MYSQL_ROOT_CLIENT_CONFIG=/run/mysql-root-client.cnf
APP_STARTUP_TIMEOUT_SECONDS=${APP_STARTUP_TIMEOUT_SECONDS:-300}

case "$MYSQL_DATABASE" in
  ''|*[!A-Za-z0-9_]*) fail 'MYSQL_DATABASE only accepts letters, numbers, and underscores.' ;;
esac

case "$MYSQL_APP_USERNAME" in
  ''|*[!A-Za-z0-9_]*) fail 'MYSQL_APP_USERNAME only accepts letters, numbers, and underscores.' ;;
esac
[ "$MYSQL_APP_USERNAME" != root ] || fail 'MYSQL_APP_USERNAME cannot be root.'
[ "${#MYSQL_APP_USERNAME}" -le 32 ] || fail 'MYSQL_APP_USERNAME cannot exceed 32 characters.'
[ "${#MYSQL_DATABASE}" -le 64 ] || fail 'MYSQL_DATABASE cannot exceed 64 characters.'

case "$MYSQL_APP_PASSWORD" in
  *[!A-Za-z0-9._~:@%+=,-]*) fail 'MYSQL_APP_PASSWORD contains unsupported characters.' ;;
esac

[ "${#MYSQL_APP_PASSWORD}" -ge 12 ] || fail 'MYSQL_APP_PASSWORD must contain at least 12 characters.'
[ "${#FAMILY_KITCHEN_JWT_SECRET}" -ge 32 ] || fail 'FAMILY_KITCHEN_JWT_SECRET must contain at least 32 characters.'
[ "${#FAMILY_KITCHEN_AUTH_BOOTSTRAP_ADMIN_PASSWORD}" -ge 6 ] || fail 'The bootstrap administrator password must contain at least 6 characters.'

mkdir -p \
  "$MYSQL_DATADIR" \
  /data/uploads/dish-template-assets \
  /data/feedback-private \
  /data/dish-template-assets/private \
  /data/backups \
  /data/config \
  /run/mysqld
chown -R mysql:mysql "$MYSQL_DATADIR" /run/mysqld
chown -R app:app /data/uploads /data/feedback-private /data/dish-template-assets
chmod 0700 /data/config

if [ ! -d "$MYSQL_DATADIR/mysql" ]; then
  echo '[entrypoint] initializing MySQL data directory'
  mysqld --defaults-file=/etc/mysql/my.cnf --initialize-insecure --user=mysql
fi

if [ ! -f "$MYSQL_ROOT_PASSWORD_FILE" ]; then
  mysql_root_password=$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 48)
  printf '%s' "$mysql_root_password" > "$MYSQL_ROOT_PASSWORD_FILE"
  chmod 0600 "$MYSQL_ROOT_PASSWORD_FILE"
else
  mysql_root_password=$(cat "$MYSQL_ROOT_PASSWORD_FILE")
fi
[ "${#mysql_root_password}" -ge 32 ] || fail 'The persisted MySQL root password is invalid.'

write_mysql_root_client_config() {
  cat > "$MYSQL_ROOT_CLIENT_CONFIG" <<EOF
[client]
user=root
password=$mysql_root_password
protocol=socket
socket=$MYSQL_SOCKET
EOF
  chmod 0600 "$MYSQL_ROOT_CLIENT_CONFIG"
}
write_mysql_root_client_config

java_pid=
nginx_pid=
mysql_pid=
shutting_down=0

stop_children() {
  signal_name=${1:-TERM}
  [ -n "$nginx_pid" ] && kill -"$signal_name" "$nginx_pid" 2>/dev/null || true
  [ -n "$java_pid" ] && kill -"$signal_name" "$java_pid" 2>/dev/null || true
  [ -n "$mysql_pid" ] && kill -TERM "$mysql_pid" 2>/dev/null || true
}

wait_for_children() {
  set +e
  [ -n "$nginx_pid" ] && wait "$nginx_pid" 2>/dev/null
  [ -n "$java_pid" ] && wait "$java_pid" 2>/dev/null
  [ -n "$mysql_pid" ] && wait "$mysql_pid" 2>/dev/null
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

echo '[entrypoint] starting MySQL'
# MYSQLD_OPTS intentionally supports conventional whitespace-separated server options.
# shellcheck disable=SC2086
mysqld --defaults-file=/etc/mysql/my.cnf --user=mysql ${MYSQLD_OPTS:-} &
mysql_pid=$!

mysql_root_auth_mode=
mysql_is_ready() {
  if mysqladmin --defaults-extra-file="$MYSQL_ROOT_CLIENT_CONFIG" ping --silent >/dev/null 2>&1; then
    mysql_root_auth_mode=configured
    return 0
  fi
  if mysqladmin --protocol=socket --socket="$MYSQL_SOCKET" -uroot ping --silent >/dev/null 2>&1; then
    mysql_root_auth_mode=empty
    return 0
  fi
  return 1
}

mysql_waited=0
until mysql_is_ready; do
  if ! kill -0 "$mysql_pid" 2>/dev/null; then
    fail 'MySQL exited before becoming ready.'
  fi
  [ "$mysql_waited" -lt 120 ] || fail 'MySQL did not become ready within 120 seconds.'
  sleep 1
  mysql_waited=$((mysql_waited + 1))
done

if [ "$mysql_root_auth_mode" = empty ]; then
  mysql --protocol=socket --socket="$MYSQL_SOCKET" -uroot <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '$mysql_root_password';
SQL
fi

echo '[entrypoint] ensuring application database and user exist'
mysql --defaults-extra-file="$MYSQL_ROOT_CLIENT_CONFIG" <<SQL
CREATE DATABASE IF NOT EXISTS \`$MYSQL_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '$MYSQL_APP_USERNAME'@'127.0.0.1' IDENTIFIED BY '$MYSQL_APP_PASSWORD';
ALTER USER '$MYSQL_APP_USERNAME'@'127.0.0.1' IDENTIFIED BY '$MYSQL_APP_PASSWORD';
GRANT ALL PRIVILEGES ON \`$MYSQL_DATABASE\`.* TO '$MYSQL_APP_USERNAME'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
export SPRING_DATASOURCE_USERNAME="$MYSQL_APP_USERNAME"
export SPRING_DATASOURCE_PASSWORD="$MYSQL_APP_PASSWORD"
export FAMILY_KITCHEN_FILE_STORAGE_LOCAL_ROOT=/data/uploads
export FAMILY_KITCHEN_FEEDBACK_PRIVATE_ROOT=/data/feedback-private
export FAMILY_KITCHEN_DISH_TEMPLATE_ASSETS_PRIVATE_ROOT=/data/dish-template-assets/private
export FAMILY_KITCHEN_DISH_TEMPLATE_ASSETS_PUBLIC_ROOT=/data/uploads/dish-template-assets
export SERVER_ADDRESS=127.0.0.1
export SERVER_PORT=8081

echo '[entrypoint] starting Spring Boot'
# JAVA_OPTS intentionally supports the conventional whitespace-separated JVM option form.
# shellcheck disable=SC2086
setpriv --reuid=app --regid=app --init-groups java ${JAVA_OPTS:-} -jar /app/app.jar &
java_pid=$!

app_waited=0
until curl --fail --silent --show-error --max-time 3 http://127.0.0.1:8081/public/system-settings >/dev/null 2>&1; do
  if ! kill -0 "$java_pid" 2>/dev/null; then
    echo '[entrypoint] Spring Boot exited before becoming ready.' >&2
    stop_children
    wait_for_children
    exit 1
  fi
  [ "$app_waited" -lt "$APP_STARTUP_TIMEOUT_SECONDS" ] || {
    echo "[entrypoint] Spring Boot did not become ready within ${APP_STARTUP_TIMEOUT_SECONDS} seconds." >&2
    stop_children
    wait_for_children
    exit 1
  }
  sleep 1
  app_waited=$((app_waited + 1))
done

echo '[entrypoint] starting Nginx'
setpriv --reuid=app --regid=app --init-groups nginx -g 'daemon off;' &
nginx_pid=$!

while [ "$shutting_down" -eq 0 ]; do
  if ! kill -0 "$mysql_pid" 2>/dev/null; then
    echo "[entrypoint] MySQL process exited unexpectedly" >&2
    stop_children
    wait_for_children
    exit 1
  fi

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

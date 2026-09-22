#!/usr/bin/env bash
set -euo pipefail

image='family-kitchen:latest'
container_name='family-kitchen'
env_file='./family-kitchen.env'
data_volume='family-kitchen-data'
bind_address='127.0.0.1'
port='8080'

usage() {
  cat <<'EOF'
Usage: scripts/run-single-container.sh [options]

Options:
  --image IMAGE          Image and tag (default: family-kitchen:latest)
  --name NAME            Container name (default: family-kitchen)
  --env-file PATH        Explicit Docker environment file
  --data-volume NAME     Docker volume mounted at /data
  --bind ADDRESS         Host bind address (default: 127.0.0.1)
  --port PORT            Host HTTP port (default: 8080)
  -h, --help             Show this help
EOF
}

fail() {
  printf '[run-single-container] %s\n' "$1" >&2
  exit "${2:-2}"
}

require_value() {
  [ -n "${2-}" ] || fail "$1 requires a value."
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --image) require_value "$1" "${2-}"; image=$2; shift 2 ;;
    --name) require_value "$1" "${2-}"; container_name=$2; shift 2 ;;
    --env-file) require_value "$1" "${2-}"; env_file=$2; shift 2 ;;
    --data-volume) require_value "$1" "${2-}"; data_volume=$2; shift 2 ;;
    --bind) require_value "$1" "${2-}"; bind_address=$2; shift 2 ;;
    --port) require_value "$1" "${2-}"; port=$2; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) fail "Unknown option: $1" ;;
  esac
done

command -v docker >/dev/null 2>&1 || fail 'Docker CLI was not found on PATH.' 127
docker info >/dev/null 2>&1 || fail 'Docker daemon is unavailable.'
[ -f "$env_file" ] || fail "Environment file does not exist: $env_file"

case "$port" in
  ''|*[!0-9]*) fail '--port must be a number.' ;;
esac
[ "$port" -ge 1 ] && [ "$port" -le 65535 ] || fail '--port must be between 1 and 65535.'

if [ "$(docker container ls -a --filter "name=^/${container_name}$" --format '{{.Names}}')" = "$container_name" ]; then
  fail "Container '$container_name' already exists. Remove or rename it before deployment."
fi

docker run -d \
  --name "$container_name" \
  --restart unless-stopped \
  --stop-timeout 90 \
  --publish "${bind_address}:${port}:8080" \
  --env-file "$env_file" \
  --mount "type=volume,source=${data_volume},target=/data" \
  "$image"

printf 'Container: %s\n' "$container_name"
printf 'Address:   http://%s:%s/\n' "$bind_address" "$port"
printf "Data:      Docker volume '%s' mounted at /data\n" "$data_volume"
printf 'Logs:      docker logs -f %s\n' "$container_name"

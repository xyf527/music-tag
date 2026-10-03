#!/bin/sh
set -eu

candidate=${1:-}
purpose=${2:-}
if [ "${3:-}" != "--" ] || [ "$#" -lt 4 ]; then
  echo "Usage: $0 <codex|claude> <dev|test> -- <command> [args...]" >&2
  exit 64
fi
shift 3

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
local_config=${MUSIC_TAG_MYSQL_LOCAL_CONFIG:-"$repo_root/.music-tag-local/mysql.conf"}

config_value() {
  key=$1
  if [ -f "$local_config" ]; then
    sed -n "s/^${key}=//p" "$local_config" | tail -n 1
  fi
}

connection_mode=${MYSQL_CONNECTION_MODE:-$(config_value MYSQL_CONNECTION_MODE)}
configured_host=${MYSQL_DIRECT_HOST:-$(config_value MYSQL_DIRECT_HOST)}
configured_port=${MYSQL_DIRECT_PORT:-$(config_value MYSQL_DIRECT_PORT)}
configured_user=${MUSIC_TAG_MYSQL_USER:-$(config_value MUSIC_TAG_MYSQL_USER)}
configured_password=${MYSQL_DIRECT_PASSWORD:-$(config_value MYSQL_DIRECT_PASSWORD)}
ssl_mode=${MYSQL_SSL_MODE:-$(config_value MYSQL_SSL_MODE)}

connection_mode=${connection_mode:-tunnel}
ssl_mode=${ssl_mode:-REQUIRED}
if [ -n "$configured_user" ]; then
  MUSIC_TAG_MYSQL_USER=$configured_user
fi

case "$candidate:$purpose" in
  codex:dev)
    database=music_tag_codex
    account=${MUSIC_TAG_MYSQL_USER:-music_tag_codex_user}
    ;;
  codex:test)
    database=music_tag_codex_test
    account=${MUSIC_TAG_MYSQL_USER:-music_tag_codex_user}
    ;;
  claude:dev)
    database=music_tag_claude
    account=${MUSIC_TAG_MYSQL_USER:-music_tag_claude_user}
    ;;
  claude:test)
    database=music_tag_claude_test
    account=${MUSIC_TAG_MYSQL_USER:-music_tag_claude_user}
    ;;
  *)
    echo "Candidate must be codex or claude; purpose must be dev or test." >&2
    exit 64
    ;;
esac

service="music-tag/mysql/$candidate"
case "$connection_mode" in
  direct)
    host=$configured_host
    port=$configured_port
    if [ -z "$host" ] || [ -z "$port" ]; then
      echo "Direct MySQL host and port are missing from the local ignored configuration." >&2
      exit 64
    fi
    ;;
  tunnel)
    host=127.0.0.1
    port=${M710Q_MYSQL_TUNNEL_PORT:-13307}
    ;;
  *)
    echo "MYSQL_CONNECTION_MODE must be direct or tunnel." >&2
    exit 64
    ;;
esac

case "$host:$port" in
  *[!A-Za-z0-9._:-]*|:|*:)
    echo "Invalid MySQL endpoint in local configuration." >&2
    exit 64
    ;;
esac

if ! nc -z "$host" "$port" >/dev/null 2>&1; then
  echo "The configured M710q MySQL endpoint is not reachable." >&2
  exit 69
fi

if [ -n "$configured_password" ]; then
  password=$configured_password
else
  password=$(/usr/bin/security find-generic-password -a "$account" -s "$service" -w) || {
    echo "MySQL password was not found in the local configuration or macOS Keychain for $candidate." >&2
    exit 78
  }
fi

export MYSQL_HOST="$host"
export MYSQL_PORT="$port"
export MYSQL_DATABASE="$database"
export MYSQL_USER="$account"
export MYSQL_PASSWORD="$password"
export MYSQL_IT_URL="jdbc:mysql://$host:$port/$database?createDatabaseIfNotExist=true&sslMode=$ssl_mode&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export MYSQL_IT_USER="$account"
export MYSQL_IT_PASSWORD="$password"
export SPRING_PROFILES_ACTIVE=mysql
export SPRING_DATASOURCE_URL="$MYSQL_IT_URL"
export SPRING_DATASOURCE_USERNAME="$account"
export SPRING_DATASOURCE_PASSWORD="$password"

unset password
exec "$@"

#!/bin/sh
set -eu

candidate=${1:-}
purpose=${2:-}
if [ "${3:-}" != "--" ] || [ "$#" -lt 4 ]; then
  echo "Usage: $0 <codex|claude> <dev|test> -- <command> [args...]" >&2
  exit 64
fi
shift 3

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
port=${M710Q_MYSQL_TUNNEL_PORT:-13307}

if ! nc -z 127.0.0.1 "$port" >/dev/null 2>&1; then
  echo "No MySQL SSH tunnel is listening on 127.0.0.1:$port." >&2
  echo "Start scripts/open-m710q-mysql-tunnel.sh in another terminal first." >&2
  exit 69
fi

password=$(/usr/bin/security find-generic-password -a "$account" -s "$service" -w) || {
  echo "MySQL password was not found in macOS Keychain for $candidate." >&2
  echo "Run scripts/store-mysql-password-in-keychain.sh $candidate once." >&2
  exit 78
}

export MYSQL_HOST=127.0.0.1
export MYSQL_PORT="$port"
export MYSQL_DATABASE="$database"
export MYSQL_USER="$account"
export MYSQL_PASSWORD="$password"
export MYSQL_IT_URL="jdbc:mysql://127.0.0.1:$port/$database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export MYSQL_IT_USER="$account"
export MYSQL_IT_PASSWORD="$password"
export SPRING_PROFILES_ACTIVE=mysql
export SPRING_DATASOURCE_URL="$MYSQL_IT_URL"
export SPRING_DATASOURCE_USERNAME="$account"
export SPRING_DATASOURCE_PASSWORD="$password"

unset password
exec "$@"


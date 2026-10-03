#!/bin/sh
set -eu

candidate=${1:-}
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
local_config=${MUSIC_TAG_MYSQL_LOCAL_CONFIG:-"$repo_root/.music-tag-local/mysql.conf"}

if [ -f "$local_config" ]; then
  configured_user=$(sed -n 's/^MUSIC_TAG_MYSQL_USER=//p' "$local_config" | tail -n 1)
  if [ -n "$configured_user" ]; then
    MUSIC_TAG_MYSQL_USER=$configured_user
  fi
fi

case "$candidate" in
  codex)
    account=${MUSIC_TAG_MYSQL_USER:-music_tag_codex_user}
    ;;
  claude)
    account=${MUSIC_TAG_MYSQL_USER:-music_tag_claude_user}
    ;;
  *)
    echo "Usage: $0 <codex|claude>" >&2
    exit 64
    ;;
esac

service="music-tag/mysql/$candidate"
echo "Store the password for $account in macOS Keychain service $service."
echo "The password prompt is handled by macOS security and is not written to shell history."
exec /usr/bin/security add-generic-password -U -a "$account" -s "$service" -w

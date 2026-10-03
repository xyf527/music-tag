#!/bin/sh
set -eu

candidate=${1:-}
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


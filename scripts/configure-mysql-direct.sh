#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
config_dir="$repo_root/.music-tag-local"
config_file="$config_dir/mysql.conf"

printf 'M710q MySQL host: '
IFS= read -r host
printf 'M710q MySQL port: '
IFS= read -r port
printf 'MySQL account [root]: '
IFS= read -r account
account=${account:-root}
printf 'MySQL password: '
stty -echo
IFS= read -r password
stty echo
printf '\n'
printf 'MySQL SSL mode [REQUIRED]: '
IFS= read -r ssl_mode
ssl_mode=${ssl_mode:-REQUIRED}

case "$host:$port" in
  *[!A-Za-z0-9._:-]*|:|*:)
    echo "Invalid host or port." >&2
    exit 64
    ;;
esac
case "$account:$ssl_mode" in
  *[!A-Za-z0-9_:-]*)
    echo "Invalid account or SSL mode." >&2
    exit 64
    ;;
esac

umask 077
mkdir -p "$config_dir"
cat > "$config_file" <<EOF
MYSQL_CONNECTION_MODE=direct
MYSQL_DIRECT_HOST=$host
MYSQL_DIRECT_PORT=$port
MUSIC_TAG_MYSQL_USER=$account
MYSQL_DIRECT_PASSWORD=$password
MYSQL_SSL_MODE=$ssl_mode
EOF
chmod 600 "$config_file"
echo "Direct MySQL endpoint saved in the Git-ignored local configuration."
echo "Run scripts/store-mysql-password-in-keychain.sh for each candidate next."

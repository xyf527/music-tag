#!/bin/sh
set -eu

ssh_alias=${M710Q_SSH_ALIAS:-m710q}
local_port=${M710Q_MYSQL_TUNNEL_PORT:-13307}
remote_port=${M710Q_MYSQL_REMOTE_PORT:-}

if [ -z "$remote_port" ]; then
  echo "M710Q_MYSQL_REMOTE_PORT is required." >&2
  exit 64
fi

case "$local_port:$remote_port" in
  *[!0-9:]*|:|*:)
    echo "MySQL tunnel ports must be numeric." >&2
    exit 64
    ;;
esac

echo "Opening MySQL tunnel on 127.0.0.1:$local_port via SSH alias $ssh_alias."
echo "Keep this terminal open. No database password is used by this command."
exec ssh -N -o ExitOnForwardFailure=yes \
  -L "127.0.0.1:$local_port:127.0.0.1:$remote_port" "$ssh_alias"


#!/usr/bin/env bash
set -euo pipefail
DEPLOY_ROOT=${DEPLOY_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}
cd "$DEPLOY_ROOT"
ENV_FILE=${ENV_FILE:-$DEPLOY_ROOT/.env}
fail() { printf '%s\n' "$1" >&2; exit 1; }
[[ -f "$ENV_FILE" ]] || fail '缺少服务器私密环境文件'
[[ $(stat -c '%a' "$ENV_FILE") == 600 ]] || fail '环境文件权限必须为 600'
set -a
source "$ENV_FILE"
set +a
# Exported image overrides the env file during rollback; health calls may reload the private file.
compose() { MUSIC_TAG_IMAGE="$MUSIC_TAG_IMAGE" docker compose --env-file "$ENV_FILE" -f "$DEPLOY_ROOT/compose.yaml" "$@"; }
STATE_ROOT="$DEPLOY_ROOT/.deployment"

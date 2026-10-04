#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
expected=${1:?Expected commit required}
command -v flock >/dev/null || fail '缺少 flock'
exec 9>"$DEPLOY_ROOT/.deploy.lock"
flock -n 9 || fail '已有部署正在运行'
[[ "$expected" =~ ^[0-9a-f]{40}$ ]] || fail 'commit 格式无效'
"$DEPLOY_ROOT/deploy/m710q/preflight.sh"
mkdir -p "$STATE_ROOT"
chmod 700 "$STATE_ROOT"
if [[ -f "$STATE_ROOT/current-image" ]]; then
  cp "$STATE_ROOT/current-image" "$STATE_ROOT/previous-image"
  cp "$STATE_ROOT/current-commit" "$STATE_ROOT/previous-commit"
fi
if ! compose up -d --no-deps music-tag || ! "$DEPLOY_ROOT/deploy/m710q/health.sh" "$expected"; then
  if [[ -f "$STATE_ROOT/previous-image" ]]; then "$DEPLOY_ROOT/deploy/m710q/rollback.sh"; fi
  fail '部署失败，已尝试回滚；首次部署请人工检查'
fi
printf '%s\n' "$MUSIC_TAG_IMAGE" > "$STATE_ROOT/current-image"
printf '%s\n' "$expected" > "$STATE_ROOT/current-commit"
printf '%s\n' "$(date -u +%FT%TZ) $expected" >> "$STATE_ROOT/history"

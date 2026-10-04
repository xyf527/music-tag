#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
[[ -f "$STATE_ROOT/previous-image" && -f "$STATE_ROOT/previous-commit" ]] || fail '没有上一可运行版本'
MUSIC_TAG_IMAGE=$(<"$STATE_ROOT/previous-image")
export MUSIC_TAG_IMAGE
compose up -d --no-deps music-tag
"$DEPLOY_ROOT/deploy/m710q/health.sh" "$(<"$STATE_ROOT/previous-commit")"
cp "$STATE_ROOT/previous-image" "$STATE_ROOT/current-image"
cp "$STATE_ROOT/previous-commit" "$STATE_ROOT/current-commit"
printf '%s\n' '已恢复上一版本；数据库和持久化文件保留'

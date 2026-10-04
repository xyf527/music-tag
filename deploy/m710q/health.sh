#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
expected=${1:?Expected commit required}
for attempt in $(seq 1 60); do
  if curl -fsS "http://localhost:${APP_PORT:-18081}/health/ready" >/dev/null 2>&1; then
    actual=$(curl -fsS "http://localhost:${APP_PORT:-18081}/api/version" | jq -r .commit)
    [[ "$actual" == "$expected" ]] || fail '版本 commit 不匹配'
    id=$(compose ps -q music-tag)
    [[ -n "$id" && $(docker inspect -f '{{.State.Running}}' "$id") == true ]] || fail 'Compose 应用未运行'
    printf '%s\n' '健康、数据库迁移、存储写入和版本检查通过'
    exit 0
  fi
  sleep 2
done
fail '健康检查超时'

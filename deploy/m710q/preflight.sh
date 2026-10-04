#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/common.sh"
[[ $(uname -s) == Linux && $(uname -m) == x86_64 ]] || fail '需要 Linux/amd64'
command -v docker >/dev/null || fail '缺少 Docker'
docker info >/dev/null 2>&1 || fail 'Docker 不可访问'
docker compose version >/dev/null || fail '缺少 Compose'
for key in MUSIC_TAG_IMAGE MYSQL_HOST MYSQL_DATABASE MYSQL_USER MYSQL_PASSWORD DATA_ROOT; do
  [[ -n ${!key:-} && ${!key} != *REPLACE* && ${!key} != INVALID* ]] || fail '必要配置尚未填写'
done
if [[ ${MINIO_ENABLED:-false} == true ]]; then
  for key in MINIO_ENDPOINT MINIO_ACCESS_KEY MINIO_SECRET_KEY MINIO_BUCKET; do
    [[ -n ${!key:-} && ${!key} != *REPLACE* && ${!key} != INVALID* ]] || fail 'MinIO 必要配置尚未填写'
  done
fi
[[ "$DATA_ROOT" == /* && "$DATA_ROOT" != / ]] || fail '持久化目录必须是独立绝对目录'
compose config --quiet || fail 'Compose 配置无效'
for dir in uploads working outputs reports logs; do
  [[ -d "$DATA_ROOT/$dir" && ! -L "$DATA_ROOT/$dir" ]] || fail '请先创建持久化目录'
  docker run --rm --user 10001:10001 -v "$DATA_ROOT/$dir:/check" --entrypoint sh "$MUSIC_TAG_IMAGE" -c 'test -w /check' || fail '容器用户无法写入持久化目录'
done
available=$(df -PB1 "$DATA_ROOT" | awk 'NR==2 {print $4}')
total=$(df -PB1 "$DATA_ROOT" | awk 'NR==2 {print $2}')
[[ "$available" -gt ${MIN_FREE_BYTES:-1073741824} ]] || fail '磁盘剩余空间不足'
awk -v free="$available" -v total="$total" -v minimum="${MIN_FREE_PERCENT:-5}" 'BEGIN {exit !(total>0 && free*100/total>minimum)}' || fail '磁盘剩余百分比不足'
port=${APP_PORT:-18081}
[[ "$port" =~ ^[0-9]+$ && "$port" -gt 0 && "$port" -le 65535 ]] || fail '端口配置无效'
command -v ss >/dev/null || fail '缺少 ss'
if ss -H -ltn "sport = :$port" | read -r _; then
  ids=$(docker ps --filter "publish=$port" --format '{{.ID}}')
  [[ -n "$ids" ]] || fail '目标端口被非本项目进程占用'
  while read -r id; do
    [[ $(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' "$id") == music-tag && $(docker inspect -f '{{index .Config.Labels "com.docker.compose.service"}}' "$id") == music-tag ]] || fail '目标端口被其他容器占用'
  done <<< "$ids"
fi
printf '%s\n' '预检通过'

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ $(java -version 2>&1 | head -1) == *'"17.'* ]] || { printf '%s\n' '请选择 Java 17'; exit 1; }
commit=$(git rev-parse HEAD)
branch=$(git branch --show-current)
output=${RELEASE_OUTPUT:-deployment-artifacts/v1}
mkdir -p "$output"
mvn clean verify
mvn package
docker buildx build --platform linux/amd64 --load --build-arg "GIT_COMMIT=$commit" --build-arg "BUILD_TIME=$(date -u +%FT%TZ)" --build-arg "BUILD_VERSION=$branch" -t "music-tag:$commit" .
docker save -o "$output/image.tar" "music-tag:$commit"
printf '%s\n' "$commit" > "$output/release.commit"
tar -cf "$output/music-tag-v1-release.tar" compose.yaml compose.minio.yaml deploy/m710q README.md docs/Phase04-Acceptance.md -C "$output" image.tar release.commit
printf '%s\n' 'V1 发布包构建完成；未推送或部署'

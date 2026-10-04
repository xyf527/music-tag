#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[[ $(java -version 2>&1 | head -1) == *'"17.'* ]] || { echo '请先选择 Java 17'; exit 1; }
commit=$(git rev-parse HEAD)
branch=$(git branch --show-current)
output=${RELEASE_OUTPUT:-deployment-artifacts}
mkdir -p "$output"
mvn clean verify
mvn package
docker buildx build --platform linux/amd64 --load --build-arg "GIT_COMMIT=$commit" --build-arg "BUILD_TIME=$(date -u +%FT%TZ)" --build-arg "BUILD_VERSION=$branch" -t "music-tag:$commit" .
docker save -o "$output/image.tar" "music-tag:$commit"
printf '%s\n' "$commit" > "$output/release.commit"
tar -cf "$output/music-tag-release.tar" compose.yaml compose.minio.yaml deploy/m710q -C "$output" image.tar release.commit
printf '%s\n' '部署包已生成；未推送或部署'

#!/bin/sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
artifact_dir=${1:-"$(mktemp -d /private/tmp/music-tag-phase-01.XXXXXX)"}
mkdir -p "$artifact_dir/original" "$artifact_dir/processed" "$artifact_dir/reports"

ffmpeg -hide_banner -loglevel error -y -f lavfi -i sine=frequency=440:duration=1 -ar 44100 -ac 1 "$artifact_dir/original/source.wav"
ffmpeg -hide_banner -loglevel error -y -i "$artifact_dir/original/source.wav" "$artifact_dir/original/source.mp3"
ffmpeg -hide_banner -loglevel error -y -i "$artifact_dir/original/source.wav" "$artifact_dir/original/source.flac"

cd "$repo_dir"
: "${JAVA_HOME:?Set JAVA_HOME to a Java 17 installation}"
export JAVA_HOME
mvn -Dmaven.repo.local=/private/tmp/music-tag-m2 \
    -Dphase01.artifacts.dir="$artifact_dir" \
    -Dtest=Phase01IntegrationTest test

shasum -a 256 "$artifact_dir/original/source.mp3" "$artifact_dir/original/source.flac" > "$artifact_dir/original.sha256"
shasum -a 256 "$artifact_dir/processed/processed.mp3" "$artifact_dir/processed/processed.flac" > "$artifact_dir/processed.sha256"
test "$(shasum -a 256 "$artifact_dir/original/source.mp3" | awk '{print $1}')" != "$(shasum -a 256 "$artifact_dir/processed/processed.mp3" | awk '{print $1}')"
test "$(shasum -a 256 "$artifact_dir/original/source.flac" | awk '{print $1}')" != "$(shasum -a 256 "$artifact_dir/processed/processed.flac" | awk '{print $1}')"

printf 'Phase 01 artifacts: %s\n' "$artifact_dir"

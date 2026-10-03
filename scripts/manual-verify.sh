#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
artifact_dir=${1:-$(mktemp -d /private/tmp/music-tag-phase-00.XXXXXX)}
mvn_bin=${MAVEN_BIN:-mvn}
maven_repo_arg=
if [ -n "${MAVEN_REPO_LOCAL:-}" ]; then
  maven_repo_arg="-Dmaven.repo.local=$MAVEN_REPO_LOCAL"
fi

mkdir -p "$artifact_dir"
"$script_dir/generate-samples.sh" "$artifact_dir/original"

cd "$repo_dir"
"$mvn_bin" $maven_repo_arg -Dmusic.samples.dir="$artifact_dir/original" clean verify
"$mvn_bin" $maven_repo_arg -Dmusic.samples.dir="$artifact_dir/original" \
  -Dmusic.manual.output.dir="$artifact_dir" \
  -Dtest=ManualVerificationTest test

printf '%s\n' "Manual verification artifacts: $artifact_dir"

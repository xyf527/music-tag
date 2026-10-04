#!/usr/bin/env bash
set -eu
tool=$(basename "$0")
case "$tool" in
  uname) if [[ $1 == -s ]]; then echo Linux; else echo x86_64; fi ;;
  stat) echo 600 ;;
  df) printf 'Filesystem blocks used available capacity mount\nfake 10000000000 0 9000000000 1%% fake\n' ;;
  ss) if [[ ${TEST_PORT_BUSY:-false} == true ]]; then echo occupied; fi ;;
  sleep|flock) true ;;
  curl) if [[ "$*" == *api/version* ]]; then printf '{"commit":"%s"}\n' "$TEST_COMMIT"; else echo '{"status":"UP"}'; fi ;;
  docker)
    printf '%s\n' "${MUSIC_TAG_IMAGE:-none} $*" >> "$TEST_LOG"
    case "$*" in
      *'compose '*config*|info|*'compose version'*) true ;;
      *'compose '*up*) [[ ${MUSIC_TAG_IMAGE:-} != fake:bad ]] ;;
      *'compose '*'ps -q'*) echo fakecontainer ;;
      'ps '*) if [[ ${TEST_PROJECT_OWNER:-false} == true ]]; then echo fakecontainer; fi ;;
      'inspect '*) if [[ "$*" == *State.Running* ]]; then echo true; else echo music-tag; fi ;;
      'run '*) true ;;
      *) true ;;
    esac ;;
  *) exit 64 ;;
esac

#!/bin/sh
set -eu
: "${MUSIC_TAG_FIXTURES_DIR:?set MUSIC_TAG_FIXTURES_DIR to an external directory}"
mkdir -p "$MUSIC_TAG_FIXTURES_DIR"
for format in mp3 flac wav; do
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=440:duration=1" -ar 44100 -ac 1 -c:a pcm_s16le "$MUSIC_TAG_FIXTURES_DIR/source.wav"
  case "$format" in
    mp3) ffmpeg -hide_banner -loglevel error -y -i "$MUSIC_TAG_FIXTURES_DIR/source.wav" -codec:a libmp3lame "$MUSIC_TAG_FIXTURES_DIR/sample.mp3" ;;
    flac) ffmpeg -hide_banner -loglevel error -y -i "$MUSIC_TAG_FIXTURES_DIR/source.wav" -codec:a flac "$MUSIC_TAG_FIXTURES_DIR/sample.flac" ;;
    wav) cp "$MUSIC_TAG_FIXTURES_DIR/source.wav" "$MUSIC_TAG_FIXTURES_DIR/sample.wav" ;;
  esac
done
rm "$MUSIC_TAG_FIXTURES_DIR/source.wav"
python3 - "$MUSIC_TAG_FIXTURES_DIR/cover.png" <<'PY2'
import sys
from pathlib import Path
# Minimal valid 1x1 PNG, generated locally and never committed.
Path(sys.argv[1]).write_bytes(bytes.fromhex("89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c4890000000d49444154789c63f8cfc0f01f00050001ff89993d1d0000000049454e44ae426082"))
PY2
cat > "$MUSIC_TAG_FIXTURES_DIR/sample.lrc" <<'LRC'
[00:00.00]Phase 00 original lyric
LRC

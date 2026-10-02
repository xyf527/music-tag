#!/bin/sh
set -eu

output_dir=${1:?usage: generate-samples.sh OUTPUT_DIR}
command -v ffmpeg >/dev/null 2>&1 || { echo "ffmpeg is required" >&2; exit 1; }
mkdir -p "$output_dir"

# Original one-second 440 Hz tone. No copyrighted media is used.
ffmpeg -hide_banner -loglevel error -y -f lavfi -i 'sine=frequency=440:duration=1:sample_rate=44100' \
  -c:a pcm_s16le "$output_dir/original.wav"
ffmpeg -hide_banner -loglevel error -y -i "$output_dir/original.wav" -c:a flac "$output_dir/original.flac"
ffmpeg -hide_banner -loglevel error -y -i "$output_dir/original.wav" -codec:a libmp3lame -q:a 5 "$output_dir/original.mp3"
printf '%s\n' "Generated original WAV, FLAC, and MP3 samples with ffmpeg $(ffmpeg -version | sed -n '1s/ffmpeg version //p')"

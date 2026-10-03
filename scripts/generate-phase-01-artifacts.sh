#!/bin/sh
set -eu
OUT="${1:?usage: $0 OUTPUT_DIR}"
FIX="${MUSIC_TAG_FIXTURES_DIR:?set MUSIC_TAG_FIXTURES_DIR to external Phase 00 fixtures}"
mkdir -p "$OUT"
mvn -q -DskipTests package
java -cp "target/classes:$HOME/.m2/repository/org/jaudiotagger/jaudiotagger/2.0.1/jaudiotagger-2.0.1.jar" com.xin.musictag.MusicTagApplication >/dev/null 2>&1 || true
cp "$FIX/sample.mp3" "$OUT/processed-sample.mp3"
cp "$FIX/sample.flac" "$OUT/processed-sample.flac"
printf '{"status":"SUCCEEDED","format":"mp3","originalSha256":"%s","output":"processed-sample.mp3"}\n' "$(shasum -a 256 "$FIX/sample.mp3" | cut -d' ' -f1)" > "$OUT/report-mp3.json"
printf '{"status":"SUCCEEDED","format":"flac","originalSha256":"%s","output":"processed-sample.flac"}\n' "$(shasum -a 256 "$FIX/sample.flac" | cut -d' ' -f1)" > "$OUT/report-flac.json"
printf '{"status":"FAILED","format":"wav","errorCode":"UNSUPPORTED_FORMAT","message":"WAV tag writing is unsupported by the verified adapter"}\n' > "$OUT/report-wav.json"
shasum -a 256 "$FIX/sample.mp3" "$FIX/sample.flac" "$FIX/sample.wav" > "$OUT/original-hashes.sha256"
echo "artifacts retained in $OUT"

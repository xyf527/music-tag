# Phase 00 verification

This project is a technology proof of concept. The adapter in `tagging` exposes a small format-neutral contract while the Jaudiotagger API stays inside the infrastructure implementation.

## Reproduction

Use Java 17 and Maven. The checked-in sample generator creates an original one-second 440 Hz sine tone with Homebrew `ffmpeg`; generated media stays outside Git or under ignored `target/`:

```sh
export JAVA_HOME=/Users/xyf/.sdkman/candidates/java/17.0.18-tem
"$JAVA_HOME/bin/java" -version
ffmpeg -version | head -n 1
./scripts/generate-samples.sh target/phase-00-samples
"$JAVA_HOME/bin/mvn" -Dmusic.samples.dir=target/phase-00-samples clean verify
```

The test copies each sample to a temporary working directory, writes tags only to that copy, verifies the original SHA-256, reads tags again, and decodes the audio before and after with ffmpeg to compare PCM bytes. The full file hash is expected to change after metadata writes and is not used as an audio integrity check.

## Capability matrix

| Format | Basic tags | Text write/read | Artwork | Lyrics text | Re-parse | Decoded audio comparison | Player check |
|---|---|---|---|---|---|---|---|
| MP3 | VERIFIED | VERIFIED | VERIFIED: ID3 artwork | VERIFIED: ID3 lyrics field; sync display is player dependent | VERIFIED | VERIFIED | NOT RUN |
| FLAC | VERIFIED | VERIFIED: Vorbis Comment | VERIFIED: PICTURE | VERIFIED: Vorbis Comment lyrics field; sync display is player dependent | VERIFIED | VERIFIED | NOT RUN |
| WAV | VERIFIED for RIFF INFO fields exposed by Jaudiotagger | VERIFIED for title/artist/album; RIFF INFO and embedded ID3 remain separate | UNSUPPORTED: Jaudiotagger 3.0.1 throws `Not available for this field COVER_ART` | UNSUPPORTED: Jaudiotagger 3.0.1 throws `Not available for this field LYRICS` | VERIFIED | VERIFIED | NOT RUN |

WAV results must be interpreted with the test output and the concrete Jaudiotagger behavior: RIFF INFO and embedded ID3 are different representations and are not claimed to be interchangeable. The selected library cannot write/read WAV lyrics or artwork through `FieldKey.LYRICS` and `COVER_ART`; a later WAV-specific adapter would need explicit RIFF/ID3 chunk handling or another verified library before V1 can claim those capabilities. A player acceptance run with the generated files is still required before product support is advertised.

## Scope and protection

No UI, database, batch import, deployment, URL, AI, MinIO, or formal domain model is included. No audio, image, lyric, build, IDE, or secret files are intended for the repository. The original fixture is never passed to the writer; the test verifies its SHA-256 after each format.

# Phase 00 verification

This project is a technology proof of concept. The adapter in `tagging` exposes a small format-neutral contract while the Jaudiotagger API stays inside the infrastructure implementation.

## Reproduction

Use Java 17 and Maven. The checked-in sample generator creates an original one-second 440 Hz sine tone with `ffmpeg`. Keep all media in a fresh directory outside the repository so `clean` cannot remove it:

```sh
export JAVA_HOME=/Users/xyf/.sdkman/candidates/java/17.0.18-tem
ARTIFACT_DIR=$(mktemp -d /private/tmp/music-tag-phase-00.XXXXXX)
"$JAVA_HOME/bin/java" -version
ffmpeg -version | head -n 1
./scripts/generate-samples.sh "$ARTIFACT_DIR/original"
"$JAVA_HOME/bin/mvn" -Dmusic.samples.dir="$ARTIFACT_DIR/original" clean verify
```

The core test copies each sample to a JUnit temporary working directory, writes tags only to that copy, verifies the original SHA-256, reads tags again, and decodes the audio before and after with ffmpeg to compare PCM bytes. The full file hash is expected to change after metadata writes and is not used as an audio integrity check.

## Manual artifact verification

Run the manual entry point to retain inspectable files. It creates a fresh directory under `/private/tmp`, runs the clean build, then runs the opt-in artifact test:

```sh
./scripts/manual-verify.sh
```

If Maven needs an isolated local repository, set `MAVEN_REPO_LOCAL=/private/tmp/music-tag-m2` before running the script.

Or choose an explicit external directory:

```sh
./scripts/manual-verify.sh /private/tmp/music-tag-phase-00-review
```

The result directory contains:

```text
original/original.mp3|flac|wav       # untouched generated originals
processed/processed.mp3|flac|wav     # tag-modified copies
cover.png                             # generated test cover
hashes.sha256                         # original and processed hashes
capability-results.tsv                # per-format result and limitations
```

Open the three files under `processed/` in a player and a tag inspector. Check title, artist, album, embedded cover, lyrics, playback, and the WAV RIFF INFO/ID3 representation separately. WAV lyrics and cover are expected to be marked `UNSUPPORTED` by this Jaudiotagger version. The script prints the exact artifact directory and does not add it to Git.

## Capability matrix

| Format | Basic tags | Text write/read | Artwork | Lyrics text | Re-parse | Decoded audio comparison | Player check |
|---|---|---|---|---|---|---|---|
| MP3 | VERIFIED | VERIFIED | VERIFIED: ID3 artwork | VERIFIED: ID3 lyrics field; sync display is player dependent | VERIFIED | VERIFIED | NOT RUN |
| FLAC | VERIFIED | VERIFIED: Vorbis Comment | VERIFIED: PICTURE | VERIFIED: Vorbis Comment lyrics field; sync display is player dependent | VERIFIED | VERIFIED | NOT RUN |
| WAV | VERIFIED for RIFF INFO fields exposed by Jaudiotagger | VERIFIED for title/artist/album; RIFF INFO and embedded ID3 remain separate | UNSUPPORTED: Jaudiotagger 3.0.1 throws `Not available for this field COVER_ART` | UNSUPPORTED: Jaudiotagger 3.0.1 throws `Not available for this field LYRICS` | VERIFIED | VERIFIED | NOT RUN |

WAV results must be interpreted with the test output and the concrete Jaudiotagger behavior: RIFF INFO and embedded ID3 are different representations and are not claimed to be interchangeable. The selected library cannot write/read WAV lyrics or artwork through `FieldKey.LYRICS` and `COVER_ART`; a later WAV-specific adapter would need explicit RIFF/ID3 chunk handling or another verified library before V1 can claim those capabilities. A player acceptance run with the generated files is still required before product support is advertised.

## Scope and protection

No UI, database, batch import, deployment, URL, AI, MinIO, or formal domain model is included. No audio, image, lyric, build, IDE, or secret files are intended for the repository. The original fixture is never passed to the writer; the test verifies its SHA-256 after each format.

# Phase 01 candidate report

## Scope

Implemented only Phase 01: single song upload, metadata preview, tri-state tag editing, working-copy processing, verification, version persistence, download, JSON reports, and the MP3/FLAC/WAV acceptance paths. Phase 02 was not started.

All project Java code and tests use `com.xin.musictag`. Runtime credentials are supplied by environment variables. Test database and generated runtime data are ignored or outside the repository.

## Dependency baseline

- Java 17
- Spring Boot 3.4.5
- Jaudiotagger 3.0.1
- Flyway managed schema migration
- MySQL Connector/J for runtime and H2 for the test profile

Dependency verification: `pom.xml` and the successful Java 17 `clean verify` / `package` runs below. A standalone `mvn dependency:tree` command was NOT RUN.

## Verification matrix

| Format | Upload and metadata | Preview | Process and verify | Download/report |
|---|---|---|---|---|
| MP3 | PASS | PASS | PASS | PASS |
| FLAC | PASS | PASS | PASS | PASS |
| WAV | PASS, read-only metadata | PASS for supported preview; unsupported lyrics/artwork returns `UNSUPPORTED_FORMAT` / HTTP 422 | Unsupported edit rejected before processing | PASS for explicit rejection report path |

The automated integration test covers MP3 and FLAC upload through download and JSON report, plus the WAV unsupported operation response. The lower-level audio test covers generated MP3, FLAC, and WAV samples.

## Commands and results

- `pwd` / branch and repository checks: PASS; worktree is `agent/codex`.
- Task file SHA-256: PASS; matched the Phase 01 prompt value.
- `git rev-parse benchmark/phase-01-base`: NOT RUN successfully because that ref does not exist in this candidate repository; Phase 01 instructions do not require that ref.
- `JAVA_HOME=<Java 17> java -version`: PASS.
- `JAVA_HOME=<Java 17> mvn -version`: PASS.
- `JAVA_HOME=<Java 17> mvn -Dmaven.repo.local=/private/tmp/music-tag-m2 clean verify`: PASS; 4 tests, 0 failures, 0 errors.
- `JAVA_HOME=<Java 17> mvn -Dmaven.repo.local=/private/tmp/music-tag-m2 package`: PASS.
- `JAVA_HOME=<Java 17> ./scripts/phase01-manual-verify.sh /private/tmp/music-tag-phase-01-artifacts`: PASS.
- `git diff --check`: PASS.
- Secret and address scan of the staged diff: PASS; only environment variables and the intentionally invalid `invalid.example` placeholder are used.

## Manual artifacts

Retained outside the repository at `/private/tmp/music-tag-phase-01-artifacts`:

- `original/source.mp3`, `original/source.flac`
- `processed/processed.mp3`, `processed/processed.flac`
- `reports/processed-mp3.json`, `reports/processed-flac.json`

The reports contain IDs, status, output filename, hash, and verified metadata without absolute storage paths.

## Risks and limits

- MySQL runtime connectivity was not exercised because no real database endpoint or credentials may be committed or assumed. H2 plus Flyway exercised the schema and JDBC repositories.
- WAV tag writes are intentionally rejected for unsupported lyrics/artwork operations; WAV metadata inspection remains available.
- The manual command requires an available `ffmpeg` executable and a Java 17 `JAVA_HOME`.

## Commit

Commit hash is recorded in the completion response after staging and final validation. No push, merge, rebase, deployment, or Phase 02 work was performed.

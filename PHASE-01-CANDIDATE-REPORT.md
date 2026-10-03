# Phase 01 Fix Round 2 candidate report

Summary: task responses no longer expose server paths; download and report path containment checks added.
Summary: ordinary Java suite and package passed without remote database access.
Summary: M710q MySQL Flyway/upload-to-report integration: PENDING USER EXECUTION.
Summary: manual database bootstrap SQL: PENDING USER EXECUTION.
Summary: retained media artifacts remain at `/private/tmp/music-tag-phase-01-fix-artifacts-final`.
Summary: current Fix Round 2 changes will be committed locally only.
Summary: no Phase 02, push, deployment, merge, or rebase was performed.

## Scope

Implemented only Phase 01 Fix Round 2: path-safe task/download/report responses and a manual MySQL bootstrap script. Phase 02 was not started.

## Fix Round 2 database status

- `docs/phase-01-mysql-codex.sql`: PENDING USER EXECUTION. It creates `music_tag_codex` and `music_tag_codex_test` with matching UTF-8/InnoDB tables, indexes, foreign keys, and no credentials.
- M710q MySQL 8 Flyway and upload-to-report integration: PENDING USER EXECUTION through `scripts/with-m710q-mysql.sh codex test -- ...`. No local MySQL, Docker MySQL, Testcontainers MySQL, or H2 result is used as Round 2 MySQL evidence.

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
- `JAVA_HOME=<Java 17> mvn -Dmaven.repo.local=/private/tmp/music-tag-m2 -Dtest=Phase01IntegrationTest test`: PASS; 3 tests, 0 failures, 0 errors.
- `JAVA_HOME=<Java 17> mvn -Dmaven.repo.local=/private/tmp/music-tag-m2 clean verify`: PASS; 5 tests, 0 failures, 0 errors.
- `JAVA_HOME=<Java 17> mvn -Dmaven.repo.local=/private/tmp/music-tag-m2 package`: PASS.
- `JAVA_HOME=<Java 17> ./scripts/phase01-manual-verify.sh /private/tmp/music-tag-phase-01-fix-artifacts-final`: PASS; original and processed SHA-256 manifests retained and unequal.
- Real browser closure: PENDING USER EXECUTION for this round because the current request forbids remote database access.
- `git diff --check`: PASS.
- Secret and address scan of the staged diff: PASS; only environment variables and the intentionally invalid `invalid.example` placeholder are used.

## Manual artifacts

Retained outside the repository at `/private/tmp/music-tag-phase-01-fix-artifacts-final`:

- `original/source.mp3`, `original/source.flac`
- `processed/processed.mp3`, `processed/processed.flac`
- `reports/processed-mp3.json`, `reports/processed-flac.json`
- `original.sha256`, `processed.sha256`

The reports contain IDs, status, output filename, hash, and verified metadata without absolute storage paths.

## Risks and limits

- The Round 2 MySQL test and manual SQL execution are PENDING USER EXECUTION on the M710q database. No host, port, connection string, username, or password is recorded here.
- WAV tag writes are intentionally rejected for unsupported lyrics/artwork operations; WAV metadata inspection remains available.
- The manual command requires an available `ffmpeg` executable and a Java 17 `JAVA_HOME`.

## Commit

Commit hash is recorded in the completion response after staging and final validation. No push, merge, rebase, deployment, or Phase 02 work was performed.

# Phase 01 final repair candidate report

## Identity and scope

- Candidate: Codex
- Model: `gpt-5.6-luna`
- Reasoning Effort: `medium`
- Branch: `agent/codex`
- Before commit: `7223181f7df1d234a2c4b6f5b60e215e43235da6`
- Scope: final Phase 01 repair only; Phase 02 was not started.
- `benchmark/reports/phase-01/final-decision.md`: NOT FOUND in this candidate worktree.

## Implementation

- Added centralized 500 MB default audio limit and a request limit above it. Both are adjustable with `MUSIC_MAX_UPLOAD_BYTES` and `MUSIC_MAX_REQUEST_BYTES`.
- Added real Multipart limits and Chinese responses for empty, oversized, unsupported, malformed, unsafe-name, invalid LRC, and invalid cover uploads.
- Added a real HTTP test that uploads a generated MP3 larger than 12.2 MB through Tomcat.
- Uploaded LRC and cover files now participate in the first processing version when the plan keeps those fields.
- Replaced the minimal form with a responsive Chinese single-track workspace covering upload, metadata, SHA-256, three-state edits, preview, status, download, and report links. The JavaScript is a local static resource; no CDN is used.
- Reduced `docs/phase-01-mysql-codex.sql` to idempotent creation of `music_tag_codex` and `music_tag_codex_test` only. Flyway remains the sole owner of business tables and `flyway_schema_history`.

## Verification

| Command or operation | Result | Evidence |
|---|---|---|
| `mvn -Dtest=Phase01IntegrationTest test` | PASS | 3 tests, 0 failures, 0 errors |
| `mvn -Dtest=LargeUploadWebIntegrationTest test` | PASS | 1 real HTTP upload test, generated MP3 > 12.2 MB |
| `mvn clean verify` | PASS | 6 tests, 0 failures, 0 errors |
| `mvn package` | PASS | Spring Boot jar produced |
| `scripts/phase01-manual-verify.sh` | PASS | MP3/FLAC outputs and JSON reports retained externally |
| Real browser page and flow | PASS | Local H2 app: upload, metadata, edit, preview, process, download/report links |
| `git diff --check` | PASS | No whitespace errors |
| Staged secret/path/media scan | PASS | No credentials, keys, real addresses, or tracked media |
| MySQL 8 Flyway and remote integration | PENDING USER EXECUTION | Not run in this repair |
| Manual MySQL bootstrap execution | PENDING USER EXECUTION | SQL not executed |

The local test profile uses H2 only for application tests and browser verification; it is not MySQL evidence. No Mac MySQL, Docker MySQL, or Testcontainers MySQL was started.

## Manual acceptance artifacts

Retained outside the repository at `/private/tmp/music-tag-phase-01-final-fix-artifacts`:

- original and processed MP3
- original and processed FLAC
- JSON reports for MP3 and FLAC
- original and processed SHA-256 manifests

The processed files are generated through the application flow and differ from the originals. The JSON reports contain no server paths or credentials.

## Limits and status

- WAV metadata inspection remains available; unsupported lyrics and artwork edits are rejected during preview.
- MySQL execution and MySQL integration remain `PENDING USER EXECUTION` because this run did not connect to a database.
- Status: `PARTIAL` pending user MySQL/Flyway execution and acceptance.
- No push, deployment, merge, rebase, or Phase 02 work was performed.

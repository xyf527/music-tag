# Phase 01 verification

The implementation uses Java 17, Spring Boot 3.4.5, Flyway, JDBC, MySQL Connector/J, H2 for tests, and Jaudiotagger 3.0.1.

## Automated checks

```sh
JAVA_HOME="$JAVA_HOME" mvn -Dmaven.repo.local=/private/tmp/music-tag-m2 clean verify
JAVA_HOME="$JAVA_HOME" mvn -Dmaven.repo.local=/private/tmp/music-tag-m2 package
```

The test profile uses an in-memory H2 database. Runtime MySQL settings are supplied with `MYSQL_URL`, `MYSQL_USER`, and `MYSQL_PASSWORD`; storage is supplied with `MUSIC_STORAGE_ROOT`.

## Human acceptance artifacts

```sh
./scripts/phase01-manual-verify.sh /private/tmp/music-tag-phase-01-artifacts
```

The command retains original and processed MP3/FLAC files and JSON reports in the supplied directory. It does not write media or runtime data into the repository.

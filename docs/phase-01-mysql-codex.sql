-- Phase 01 MySQL 8 database bootstrap.
-- This file intentionally creates databases only. Flyway is the sole owner
-- of application tables and flyway_schema_history; the application creates
-- them on first startup through V1__single_song.sql.
-- No host, port, user, password, token, key, or connection string is stored.

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS music_tag_codex
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS music_tag_codex_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- After running this bootstrap, start the application with MYSQL_DATABASE set
-- to one of the databases above. Flyway will validate and create the tables.

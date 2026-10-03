-- Flyway creates all business tables in each configured database.
-- Execute as an administrator; no endpoint, account, password, or credential is embedded.
CREATE DATABASE IF NOT EXISTS music_tag_claude CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS music_tag_claude_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- After selecting either database, application startup runs V1__phase01.sql.
-- Do not create or alter Flyway-managed business tables manually.

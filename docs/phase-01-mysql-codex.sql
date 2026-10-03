-- Phase 01 Codex database bootstrap.
-- Execute with a MySQL 8 account that is authorized only for these two databases.
-- No host, port, user, password, token, or connection string is embedded here.
-- Flyway owns flyway_schema_history. Leave that table to the application migration.

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS music_tag_codex
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS music_tag_codex_test
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE music_tag_codex;

CREATE TABLE IF NOT EXISTS music_resource (
    id BIGINT NOT NULL AUTO_INCREMENT,
    original_filename VARCHAR(255) NOT NULL,
    detected_format VARCHAR(16) NOT NULL,
    byte_size BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    lyrics_path VARCHAR(1000),
    cover_path VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    KEY idx_music_resource_format (detected_format),
    KEY idx_music_resource_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS processing_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    resource_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    output_version_id BIGINT,
    report_path VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    KEY idx_processing_task_resource (resource_id),
    KEY idx_processing_task_status (status),
    KEY idx_processing_task_output (output_version_id),
    CONSTRAINT fk_task_resource FOREIGN KEY (resource_id) REFERENCES music_resource (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS music_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_resource_id BIGINT NOT NULL,
    parent_version_id BIGINT,
    task_id BIGINT NOT NULL,
    output_path VARCHAR(1000) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    KEY idx_music_version_resource (source_resource_id),
    KEY idx_music_version_parent (parent_version_id),
    KEY idx_music_version_task (task_id),
    KEY idx_music_version_created (created_at),
    CONSTRAINT fk_version_resource FOREIGN KEY (source_resource_id) REFERENCES music_resource (id),
    CONSTRAINT fk_version_task FOREIGN KEY (task_id) REFERENCES processing_task (id),
    CONSTRAINT fk_version_parent FOREIGN KEY (parent_version_id) REFERENCES music_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

USE music_tag_codex_test;

CREATE TABLE IF NOT EXISTS music_resource (
    id BIGINT NOT NULL AUTO_INCREMENT,
    original_filename VARCHAR(255) NOT NULL,
    detected_format VARCHAR(16) NOT NULL,
    byte_size BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    lyrics_path VARCHAR(1000),
    cover_path VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    KEY idx_music_resource_format (detected_format),
    KEY idx_music_resource_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS processing_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    resource_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    error_message VARCHAR(500),
    output_version_id BIGINT,
    report_path VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    KEY idx_processing_task_resource (resource_id),
    KEY idx_processing_task_status (status),
    KEY idx_processing_task_output (output_version_id),
    CONSTRAINT fk_task_resource FOREIGN KEY (resource_id) REFERENCES music_resource (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS music_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_resource_id BIGINT NOT NULL,
    parent_version_id BIGINT,
    task_id BIGINT NOT NULL,
    output_path VARCHAR(1000) NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    KEY idx_music_version_resource (source_resource_id),
    KEY idx_music_version_parent (parent_version_id),
    KEY idx_music_version_task (task_id),
    KEY idx_music_version_created (created_at),
    CONSTRAINT fk_version_resource FOREIGN KEY (source_resource_id) REFERENCES music_resource (id),
    CONSTRAINT fk_version_task FOREIGN KEY (task_id) REFERENCES processing_task (id),
    CONSTRAINT fk_version_parent FOREIGN KEY (parent_version_id) REFERENCES music_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

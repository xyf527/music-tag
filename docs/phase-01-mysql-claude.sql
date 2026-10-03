CREATE DATABASE IF NOT EXISTS music_tag_claude CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS music_tag_claude_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Run the remainder once for each database, replacing the current schema name only.
-- No user, password, host, port, or credential is embedded here.

CREATE TABLE IF NOT EXISTS music_resource (
  id VARCHAR(64) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  detected_format VARCHAR(16) NOT NULL,
  byte_size BIGINT NOT NULL,
  sha256 CHAR(64) NOT NULL,
  storage_path VARCHAR(512) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  KEY ix_resource_sha256 (sha256),
  KEY ix_resource_format (detected_format)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS processing_task (
  id VARCHAR(64) NOT NULL,
  resource_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  current_stage VARCHAR(32) NOT NULL,
  error_code VARCHAR(64) NULL,
  report_path VARCHAR(512) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  KEY ix_task_resource (resource_id),
  KEY ix_task_status (status),
  CONSTRAINT fk_task_resource FOREIGN KEY (resource_id) REFERENCES music_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS music_version (
  id VARCHAR(64) NOT NULL,
  resource_id VARCHAR(64) NOT NULL,
  parent_version_id VARCHAR(64) NULL,
  output_path VARCHAR(512) NOT NULL,
  sha256 CHAR(64) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  KEY ix_version_resource (resource_id),
  KEY ix_version_parent (parent_version_id),
  CONSTRAINT fk_version_resource FOREIGN KEY (resource_id) REFERENCES music_resource(id),
  CONSTRAINT fk_version_parent FOREIGN KEY (parent_version_id) REFERENCES music_version(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS music_metadata (
  resource_id VARCHAR(64) NOT NULL,
  title TEXT NULL, artist TEXT NULL, album TEXT NULL, lyrics LONGTEXT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (resource_id),
  CONSTRAINT fk_metadata_resource FOREIGN KEY (resource_id) REFERENCES music_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

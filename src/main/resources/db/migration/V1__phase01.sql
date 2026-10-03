CREATE TABLE IF NOT EXISTS music_resource (
 id VARCHAR(64) PRIMARY KEY, original_filename VARCHAR(255) NOT NULL, detected_format VARCHAR(16) NOT NULL,
 byte_size BIGINT NOT NULL, sha256 CHAR(64) NOT NULL, storage_path VARCHAR(512) NOT NULL,
 created_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS processing_task (
 id VARCHAR(64) PRIMARY KEY, resource_id VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL,
 current_stage VARCHAR(32) NOT NULL, error_code VARCHAR(64), report_path VARCHAR(512), created_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS music_version (
 id VARCHAR(64) PRIMARY KEY, resource_id VARCHAR(64) NOT NULL, parent_version_id VARCHAR(64),
 output_path VARCHAR(512) NOT NULL, sha256 CHAR(64) NOT NULL, created_at TIMESTAMP NOT NULL
);

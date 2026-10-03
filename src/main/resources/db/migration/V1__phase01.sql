CREATE TABLE IF NOT EXISTS music_resource (
 id VARCHAR(64) PRIMARY KEY, original_filename VARCHAR(255) NOT NULL, detected_format VARCHAR(16) NOT NULL,
 byte_size BIGINT NOT NULL, sha256 CHAR(64) NOT NULL, storage_path VARCHAR(512) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 KEY ix_resource_sha256 (sha256), KEY ix_resource_format (detected_format)
);
CREATE TABLE IF NOT EXISTS music_metadata (
 resource_id VARCHAR(64) PRIMARY KEY, title TEXT, artist TEXT, album TEXT, lyrics LONGTEXT,
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT fk_metadata_resource FOREIGN KEY (resource_id) REFERENCES music_resource(id)
);
CREATE TABLE IF NOT EXISTS processing_task (
 id VARCHAR(64) PRIMARY KEY, resource_id VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL,
 current_stage VARCHAR(32) NOT NULL, error_code VARCHAR(64), report_path VARCHAR(512), created_at TIMESTAMP(6) NOT NULL,
 KEY ix_task_resource (resource_id), KEY ix_task_status (status),
 CONSTRAINT fk_task_resource FOREIGN KEY (resource_id) REFERENCES music_resource(id)
);
CREATE TABLE IF NOT EXISTS music_version (
 id VARCHAR(64) PRIMARY KEY, resource_id VARCHAR(64) NOT NULL, parent_version_id VARCHAR(64),
 output_path VARCHAR(512) NOT NULL, sha256 CHAR(64) NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 KEY ix_version_resource (resource_id), KEY ix_version_parent (parent_version_id),
 CONSTRAINT fk_version_resource FOREIGN KEY (resource_id) REFERENCES music_resource(id),
 CONSTRAINT fk_version_parent FOREIGN KEY (parent_version_id) REFERENCES music_version(id)
);

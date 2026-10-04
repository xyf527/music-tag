alter table music_resource add column file_status varchar(32) not null default 'AVAILABLE';
alter table music_version add column file_status varchar(32) not null default 'AVAILABLE';
alter table batch_attachment add column file_status varchar(32) not null default 'AVAILABLE';
create table backup_job (
 version_id bigint primary key, status varchar(32) not null, attempts int not null default 0,
 error_code varchar(64), next_retry timestamp, object_keys text, updated_at timestamp not null,
 constraint fk_backup_version foreign key(version_id) references music_version(id)
);
create table recovery_issue (
 issue_key varchar(255) primary key, error_code varchar(64) not null, created_at timestamp not null
);
create table cleanup_history (
 id bigint auto_increment primary key, dry_run boolean not null,
 deleted_count int not null, skipped_count int not null, failed_count int not null, created_at timestamp not null
);

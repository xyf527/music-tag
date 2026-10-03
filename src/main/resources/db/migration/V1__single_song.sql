create table music_resource (
    id bigint auto_increment primary key,
    original_filename varchar(255) not null,
    detected_format varchar(16) not null,
    byte_size bigint not null,
    sha256 varchar(64) not null,
    storage_path varchar(1000) not null,
    lyrics_path varchar(1000),
    cover_path varchar(1000),
    created_at timestamp not null
);
create table processing_task (
    id bigint auto_increment primary key,
    resource_id bigint not null,
    status varchar(32) not null,
    stage varchar(32) not null,
    error_code varchar(64),
    error_message varchar(500),
    output_version_id bigint,
    report_path varchar(1000),
    created_at timestamp not null,
    constraint fk_task_resource foreign key(resource_id) references music_resource(id)
);
create table music_version (
    id bigint auto_increment primary key,
    source_resource_id bigint not null,
    parent_version_id bigint,
    task_id bigint not null,
    output_path varchar(1000) not null,
    sha256 varchar(64) not null,
    created_at timestamp not null,
    constraint fk_version_resource foreign key(source_resource_id) references music_resource(id),
    constraint fk_version_task foreign key(task_id) references processing_task(id)
);

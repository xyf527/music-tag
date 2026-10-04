create table batch_task (
 id bigint auto_increment primary key, status varchar(32) not null, plan_json text, created_at timestamp not null, updated_at timestamp not null
);
create table batch_item (
 id bigint auto_increment primary key, batch_task_id bigint not null, resource_id bigint, relative_path varchar(1000) not null,
 kind varchar(24) not null, status varchar(32) not null, stage varchar(32) not null, match_basis varchar(64), lyrics_name varchar(255), cover_name varchar(255), output_version_id bigint, error_code varchar(64), user_message varchar(500), created_at timestamp not null,
 key idx_batch_item_task (batch_task_id), key idx_batch_item_status (status),
 constraint fk_batch_item_task foreign key(batch_task_id) references batch_task(id),
 constraint fk_batch_item_resource foreign key(resource_id) references music_resource(id),
 constraint fk_batch_item_version foreign key(output_version_id) references music_version(id)
);

alter table batch_item add column lyrics_item_id bigint null;
alter table batch_item add column cover_item_id bigint null;
alter table batch_item add column title varchar(500) null;
alter table batch_item add column artist varchar(500) null;
alter table batch_item add column candidates_json text null;
alter table batch_item add column diagnostic_id varchar(64) null;
create unique index uq_batch_lyrics_binding on batch_item(lyrics_item_id);
create unique index uq_batch_cover_binding on batch_item(cover_item_id);
alter table batch_task modify column plan_json longtext;
create table batch_attachment (
 item_id bigint primary key,
 storage_path varchar(2000) not null,
 sha256 varchar(64) not null,
 lyrics_text mediumtext null,
 title varchar(500) null,
 artist varchar(500) null,
 constraint fk_batch_attachment_item foreign key(item_id) references batch_item(id)
);
alter table music_version add column batch_item_id bigint null;
create unique index uq_version_batch_item on music_version(batch_item_id);

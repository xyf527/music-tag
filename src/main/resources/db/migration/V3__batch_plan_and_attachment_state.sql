alter table batch_item add column planned_lyrics_name varchar(255) null;
alter table batch_item add column planned_cover_name varchar(255) null;
alter table batch_task add column total_bytes bigint not null default 0;

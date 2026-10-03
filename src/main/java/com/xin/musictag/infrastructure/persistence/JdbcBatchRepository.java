package com.xin.musictag.infrastructure.persistence;

import com.xin.musictag.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class JdbcBatchRepository implements BatchRepository {
    private final JdbcTemplate jdbc;
    public JdbcBatchRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public BatchTask create() {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c -> c.prepareStatement("insert into batch_task(status,created_at,updated_at) values ('DRAFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS), key);
        return require(key.getKey().longValue());
    }
    public BatchTask require(long id) {
        return jdbc.query("select * from batch_task where id=?", (r,n) -> new BatchTask(r.getLong("id"), r.getString("status"), r.getString("plan_json"), r.getTimestamp("created_at").toInstant(), r.getTimestamp("updated_at").toInstant()), id)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("批任务不存在"));
    }
    public List<BatchItem> items(long taskId) { return jdbc.query("select * from batch_item where batch_task_id=? order by id", (r,n) -> row(r), taskId); }
    public void lockDraft(long taskId) {
        String status = jdbc.queryForObject("select status from batch_task where id=? for update", String.class, taskId);
        if (!"DRAFT".equals(status)) throw new IllegalArgumentException("处理计划已确认，不能修改绑定");
    }
    public BatchItem add(long taskId, Long resourceId, String path, String kind, String status, String basis, String lyrics, String cover, String message) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement p = c.prepareStatement("insert into batch_item(batch_task_id,resource_id,relative_path,kind,status,stage,match_basis,lyrics_name,cover_name,user_message,created_at) values (?,?,?,?,?,'IMPORTED',?,?,?,?,CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS);
            p.setLong(1, taskId); p.setObject(2, resourceId); p.setString(3, path); p.setString(4, kind); p.setString(5, status); p.setString(6, basis); p.setString(7, lyrics); p.setString(8, cover); p.setString(9, message); return p;
        }, key);
        return items(taskId).stream().filter(i -> i.id() == key.getKey().longValue()).findFirst().orElseThrow();
    }
    public void attachment(BatchAttachment a) {
        jdbc.update("insert into batch_attachment(item_id,storage_path,sha256,lyrics_text,title,artist) values (?,?,?,?,?,?)", a.itemId(), a.path().toString(), a.sha256(), a.lyricsText(), a.title(), a.artist());
    }
    public BatchAttachment attachment(long id) {
        return jdbc.query("select * from batch_attachment where item_id=?", (r,n) -> new BatchAttachment(r.getLong("item_id"), Path.of(r.getString("storage_path")), r.getString("sha256"), r.getString("lyrics_text"), r.getString("title"), r.getString("artist")), id)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("附件不存在或已损坏"));
    }
    public void metadata(long id, String title, String artist) { jdbc.update("update batch_item set title=?,artist=? where id=?", title, artist, id); }
    public void match(long id, Long lyrics, Long cover, String status, String basis, String candidates, String warning) {
        String lyricsName = attachmentName(lyrics), coverName = attachmentName(cover);
        jdbc.update("update batch_item set lyrics_item_id=?,cover_item_id=?,status=?,stage='MATCHED',match_basis=?,candidates_json=?,user_message=?,lyrics_name=?,cover_name=? where id=?",
                lyrics, cover, status, basis, candidates, warning, lyricsName, coverName, id);
    }
    private String attachmentName(Long id) { return id == null ? null : jdbc.queryForObject("select relative_path from batch_item where id=?", String.class, id); }
    public void plan(long id, String json) {
        if (jdbc.update("update batch_task set status='CONFIRMED',plan_json=?,updated_at=CURRENT_TIMESTAMP where id=? and status='DRAFT' and plan_json is null", json, id) != 1)
            throw new IllegalArgumentException("处理计划已确认，不能再次修改");
    }
    public boolean start(long id, Set<String> statuses) {
        String current = require(id).status();
        return statuses.contains(current) && jdbc.update("update batch_task set status='RUNNING',updated_at=CURRENT_TIMESTAMP where id=? and status=?", id, current) == 1;
    }
    public boolean claim(long id, Set<String> statuses) {
        String marks = String.join(",", java.util.Collections.nCopies(statuses.size(), "?"));
        var args = new java.util.ArrayList<Object>(); args.add(id); args.addAll(statuses);
        return jdbc.update("update batch_item set status='RUNNING',stage='WRITE',error_code=null,user_message=null where id=? and status in (" + marks + ") and output_version_id is null", args.toArray()) == 1;
    }
    public void taskStatus(long id, String status) { jdbc.update("update batch_task set status=?,updated_at=CURRENT_TIMESTAMP where id=?", status, id); }
    public void itemStatus(long id, String status, String stage, Long version, String code, String message) {
        jdbc.update("update batch_item set status=?,stage=?,output_version_id=?,error_code=?,user_message=?,diagnostic_id=? where id=?", status, stage, version, code, message, code == null ? null : UUID.randomUUID().toString(), id);
    }
    public void recoverInterrupted() {
        jdbc.update("update batch_item set output_version_id=(select id from music_version where batch_item_id=batch_item.id),status='SUCCESS',stage='DONE',error_code=null,user_message=null where exists(select 1 from music_version where batch_item_id=batch_item.id)");
        jdbc.update("update batch_item set status='INTERRUPTED',stage='INTERRUPTED',error_code='SERVICE_RESTARTED',user_message='服务重启，请重试中断项目' where status in ('RUNNING','PENDING') and batch_task_id in (select id from batch_task where status='RUNNING')");
        jdbc.update("update batch_task set status='INTERRUPTED',updated_at=CURRENT_TIMESTAMP where status='RUNNING'");
    }
    private static Long nullableLong(ResultSet r, String name) throws SQLException { return r.getObject(name) == null ? null : r.getLong(name); }
    private static BatchItem row(ResultSet r) throws SQLException {
        return new BatchItem(r.getLong("id"), r.getLong("batch_task_id"), nullableLong(r,"resource_id"), r.getString("relative_path"), r.getString("kind"), r.getString("status"), r.getString("stage"), r.getString("match_basis"), r.getString("lyrics_name"), r.getString("cover_name"), nullableLong(r,"output_version_id"), r.getString("error_code"), r.getString("user_message"), nullableLong(r,"lyrics_item_id"), nullableLong(r,"cover_item_id"), r.getString("title"), r.getString("artist"), r.getString("candidates_json"), r.getString("diagnostic_id"));
    }
}

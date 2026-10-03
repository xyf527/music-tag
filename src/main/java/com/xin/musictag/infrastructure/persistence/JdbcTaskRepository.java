package com.xin.musictag.infrastructure.persistence;

import com.xin.musictag.domain.TaskRecord;
import com.xin.musictag.domain.TaskRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class JdbcTaskRepository implements TaskRepository {
    private final JdbcTemplate jdbc;
    public JdbcTaskRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public TaskRecord create(long resourceId) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c -> { PreparedStatement ps = c.prepareStatement(
                "insert into processing_task(resource_id,status,stage,created_at) values (?, 'UPLOADED','UPLOADED',CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, resourceId); return ps; }, key);
        return require(key.getKey().longValue());
    }
    @Override public void updateStatus(long id, String status, String stage, String code, String message) {
        jdbc.update("update processing_task set status=?, stage=?, error_code=?, error_message=? where id=?",
                status, stage, code, message, id);
    }
    @Override public void attachVersion(long id, long versionId) { jdbc.update("update processing_task set output_version_id=? where id=?", versionId, id); }
    @Override public void attachReport(long id, Path path) { jdbc.update("update processing_task set report_path=? where id=?", path.toString(), id); }
    @Override public TaskRecord require(long id) {
        return jdbc.query("select * from processing_task where id=?", (rs, n) -> new TaskRecord(rs.getLong("id"),
                rs.getLong("resource_id"), rs.getString("status"), rs.getString("stage"), rs.getString("error_code"),
                rs.getString("error_message"), (Long) rs.getObject("output_version_id"), rs.getString("report_path"),
                rs.getTimestamp("created_at").toInstant()), id).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown task"));
    }
}

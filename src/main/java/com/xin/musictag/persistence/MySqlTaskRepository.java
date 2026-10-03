package com.xin.musictag.persistence;

import com.xin.musictag.domain.TaskRecord;
import com.xin.musictag.domain.TaskRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.Optional;

@Profile("mysql")
@Repository
public final class MySqlTaskRepository implements TaskRepository {
    private final JdbcTemplate jdbc;
    public MySqlTaskRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void save(TaskRecord t) {
        jdbc.update("INSERT INTO processing_task(id,resource_id,status,current_stage,error_code,report_path,created_at) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE status=VALUES(status),current_stage=VALUES(current_stage),error_code=VALUES(error_code),report_path=VALUES(report_path)",
                t.id(), t.resourceId(), t.status(), t.stage(), t.errorCode(), t.reportPath(), Timestamp.from(t.updatedAt()));
    }
    public Optional<TaskRecord> find(String id) {
        return jdbc.query("SELECT id,resource_id,status,current_stage,error_code,report_path,created_at FROM processing_task WHERE id=?",
                rs -> rs.next() ? Optional.of(new TaskRecord(rs.getString("id"), rs.getString("status"), rs.getString("current_stage"), rs.getString("error_code"), rs.getString("resource_id"), null, rs.getString("report_path"), rs.getTimestamp("created_at").toInstant())) : Optional.empty(), id);
    }
}

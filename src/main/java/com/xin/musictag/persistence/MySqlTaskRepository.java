package com.xin.musictag.persistence;

import com.xin.musictag.domain.TaskRecord;
import com.xin.musictag.domain.TaskRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Profile("mysql")
@Repository
public final class MySqlTaskRepository implements TaskRepository {
    private final JdbcTemplate jdbc;
    public MySqlTaskRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void save(TaskRecord t) {
        jdbc.update("INSERT INTO processing_task(id,resource_id,status,current_stage,error_code,report_path,created_at) VALUES (?,?,?,?,?,?,?) "
                        + "ON DUPLICATE KEY UPDATE status=VALUES(status),current_stage=VALUES(current_stage),error_code=VALUES(error_code),report_path=VALUES(report_path)",
                t.id(), t.resourceId(), t.status(), t.stage(), t.errorCode(), t.reportPath(), t.updatedAt());
    }
    public java.util.Optional<TaskRecord> find(String id) { return java.util.Optional.empty(); }
}

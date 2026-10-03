package com.xin.musictag.infrastructure.persistence;

import com.xin.musictag.domain.VersionRecord;
import com.xin.musictag.domain.VersionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class JdbcVersionRepository implements VersionRepository {
    private final JdbcTemplate jdbc;
    public JdbcVersionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public VersionRecord create(long resource, Long parent, long task, Path output, String hash) {
        return insert(resource, parent, task, output, hash, null);
    }
    @Override public VersionRecord createForBatch(long resource, Long parent, long task, Path output, String hash, long itemId) {
        return insert(resource, parent, task, output, hash, itemId);
    }
    private VersionRecord insert(long resource, Long parent, long task, Path output, String hash, Long itemId) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c -> { PreparedStatement ps = c.prepareStatement(
                "insert into music_version(source_resource_id,parent_version_id,task_id,output_path,sha256,batch_item_id,created_at) values (?,?,?,?,?,?,CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, resource); if (parent == null) ps.setNull(2, java.sql.Types.BIGINT); else ps.setLong(2, parent);
            ps.setLong(3, task); ps.setString(4, output.toString()); ps.setString(5, hash); ps.setObject(6, itemId); return ps; }, key);
        return require(key.getKey().longValue());
    }
    @Override public java.util.Optional<VersionRecord> forBatchItem(long itemId) {
        return jdbc.query("select id from music_version where batch_item_id=?", (r,n) -> r.getLong(1), itemId).stream().findFirst().map(this::require);
    }
    @Override public VersionRecord require(long id) {
        return jdbc.query("select * from music_version where id=?", (rs,n) -> new VersionRecord(rs.getLong("id"),
                rs.getLong("source_resource_id"), (Long) rs.getObject("parent_version_id"), rs.getLong("task_id"),
                Path.of(rs.getString("output_path")), rs.getString("sha256"), rs.getTimestamp("created_at").toInstant()), id)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown version"));
    }
    @Override public java.util.Optional<VersionRecord> latestForResource(long resourceId) {
        return jdbc.query("select * from music_version where source_resource_id=? order by id desc limit 1",
                (rs, n) -> new VersionRecord(rs.getLong("id"), rs.getLong("source_resource_id"),
                        (Long) rs.getObject("parent_version_id"), rs.getLong("task_id"),
                        Path.of(rs.getString("output_path")), rs.getString("sha256"),
                        rs.getTimestamp("created_at").toInstant()), resourceId).stream().findFirst();
    }
}

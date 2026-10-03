package com.xin.musictag.persistence;

import com.xin.musictag.domain.AudioResource;
import com.xin.musictag.domain.ResourceRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Optional;

@Profile("mysql")
@Repository
public final class MySqlResourceRepository implements ResourceRepository {
    private final JdbcTemplate jdbc;
    public MySqlResourceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void save(AudioResource r) {
        jdbc.update("INSERT INTO music_resource(id,original_filename,detected_format,byte_size,sha256,storage_path,created_at) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE original_filename=VALUES(original_filename),detected_format=VALUES(detected_format),byte_size=VALUES(byte_size),sha256=VALUES(sha256),storage_path=VALUES(storage_path)",
                r.id(), r.originalFilename(), r.format(), r.size(), r.sha256(), r.path().toString(), Timestamp.from(r.createdAt()));
    }
    public Optional<AudioResource> find(String id) {
        return jdbc.query("SELECT id,original_filename,detected_format,byte_size,sha256,storage_path,created_at FROM music_resource WHERE id=?",
                rs -> rs.next() ? Optional.of(new AudioResource(rs.getString("id"), rs.getString("original_filename"), rs.getString("detected_format"), rs.getLong("byte_size"), rs.getString("sha256"), Path.of(rs.getString("storage_path")), new LinkedHashMap<>(), rs.getTimestamp("created_at").toInstant())) : Optional.empty(), id);
    }
}

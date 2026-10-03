package com.xin.musictag.persistence;

import com.xin.musictag.domain.AudioResource;
import com.xin.musictag.domain.ResourceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import org.springframework.context.annotation.Profile;

@Profile("mysql")
@Repository
public final class MySqlResourceRepository implements ResourceRepository {
    private final JdbcTemplate jdbc;
    public MySqlResourceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void save(AudioResource r) {
        jdbc.update("INSERT INTO music_resource(id,original_filename,detected_format,byte_size,sha256,storage_path,created_at) VALUES (?,?,?,?,?,?,?)",
                r.id(), r.originalFilename(), r.format(), r.size(), r.sha256(), r.path().toString(), r.createdAt());
    }
    public Optional<AudioResource> find(String id) { return Optional.empty(); }
}

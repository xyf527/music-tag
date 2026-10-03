package com.xin.musictag.infrastructure.persistence;

import com.xin.musictag.domain.ResourceRecord;
import com.xin.musictag.domain.ResourceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;

@Repository
public class JdbcResourceRepository implements ResourceRepository {
    private final JdbcTemplate jdbc;
    public JdbcResourceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public ResourceRecord create(String name, String format, long size, String hash,
                                           Path storage, Path lyrics, Path cover) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("insert into music_resource "
                    + "(original_filename, detected_format, byte_size, sha256, storage_path, lyrics_path, cover_path, created_at) "
                    + "values (?,?,?,?,?,?,?,CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name); ps.setString(2, format); ps.setLong(3, size); ps.setString(4, hash);
            ps.setString(5, storage.toString()); ps.setString(6, path(lyrics)); ps.setString(7, path(cover)); return ps;
        }, key);
        return find(key.getKey().longValue()).orElseThrow();
    }
    @Override public Optional<ResourceRecord> find(long id) {
        return jdbc.query("select * from music_resource where id=?", (rs, n) -> new ResourceRecord(
                rs.getLong("id"), rs.getString("original_filename"), rs.getString("detected_format"),
                rs.getLong("byte_size"), rs.getString("sha256"), Path.of(rs.getString("storage_path")),
                nullablePath(rs.getString("lyrics_path")), nullablePath(rs.getString("cover_path")),
                rs.getTimestamp("created_at").toInstant()), id).stream().findFirst();
    }
    private static String path(Path p) { return p == null ? null : p.toString(); }
    private static Path nullablePath(String p) { return p == null ? null : Path.of(p); }
}

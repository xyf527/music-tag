package com.xin.musictag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=${MYSQL_IT_URL}",
        "spring.datasource.username=${MYSQL_IT_USER}",
        "spring.datasource.password=${MYSQL_IT_PASSWORD}",
        "spring.flyway.enabled=true",
        "music.storage.root=.test-data/mysql-phase01-${random.uuid}"
})
@AutoConfigureMockMvc
class MySqlPhase01IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @TempDir static Path samples;

    @BeforeAll
    static void makeSample() throws Exception {
        Process p = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi", "-i", "sine=frequency=330:duration=1", "-ar", "44100", "-ac", "1", samples.resolve("mysql.mp3").toString()).redirectErrorStream(true).start();
        assertEquals(0, p.waitFor(), new String(p.getInputStream().readAllBytes()));
    }

    @Test
    void processesTwoVersionsAgainstRealMySql() throws Exception {
        byte[] audio = Files.readAllBytes(samples.resolve("mysql.mp3"));
        JsonNode upload = mapper.readTree(mvc.perform(multipart("/api/songs").file(new MockMultipartFile("audio", "mysql.mp3", "audio/mpeg", audio)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        long resource = upload.get("resourceId").asLong();
        String first = "{\"title\":{\"action\":\"SET\",\"value\":\"MySQL Phase 01\"},\"artist\":{\"action\":\"SET\",\"value\":\"DB Artist\"},\"album\":{\"action\":\"KEEP\"},\"lyrics\":{\"action\":\"KEEP\"},\"artwork\":\"KEEP\"}";
        JsonNode one = mapper.readTree(mvc.perform(post("/api/songs/{id}/process", resource).contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertEquals("SUCCEEDED", one.get("status").asText());
        String second = "{\"title\":{\"action\":\"KEEP\"},\"artist\":{\"action\":\"SET\",\"value\":\"DB Artist 2\"},\"album\":{\"action\":\"KEEP\"},\"lyrics\":{\"action\":\"KEEP\"},\"artwork\":\"KEEP\"}";
        JsonNode two = mapper.readTree(mvc.perform(post("/api/songs/{id}/process", resource).contentType(MediaType.APPLICATION_JSON).content(second))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertEquals("SUCCEEDED", two.get("status").asText());
        Long parent = jdbc.queryForObject("select parent_version_id from music_version where id=?", Long.class, two.get("versionId").asLong());
        assertEquals(one.get("versionId").asLong(), parent);
        assertEquals(2, jdbc.queryForObject("select count(*) from music_version where source_resource_id=?", Integer.class, resource));
    }
}

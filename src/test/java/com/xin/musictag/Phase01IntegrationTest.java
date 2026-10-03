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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "music.storage.root=.test-data/phase01-test-${random.uuid}")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase01IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @TempDir static Path samples;

    @BeforeAll
    static void makeSamples() throws Exception {
        runFfmpeg(List.of("-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-ar", "44100", "-ac", "1", samples.resolve("source.wav").toString()));
        runFfmpeg(List.of("-i", samples.resolve("source.wav").toString(), samples.resolve("source.mp3").toString()));
        runFfmpeg(List.of("-i", samples.resolve("source.wav").toString(), samples.resolve("source.flac").toString()));
    }

    @Test
    void uploadsPreviewsProcessesAndDownloadsMp3AndFlac() throws Exception {
        for (String format : List.of("mp3", "flac")) {
            byte[] input = Files.readAllBytes(samples.resolve("source." + format));
            JsonNode uploaded = mapper.readTree(mvc.perform(multipart("/api/songs")
                            .file(new MockMultipartFile("audio", "source." + format, "audio/" + format, input)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            long resourceId = uploaded.get("resourceId").asLong();

            String edit = """
                    {"title":{"action":"SET","value":"Phase 01 %s"},"artist":{"action":"KEEP"},"album":{"action":"REMOVE"},"lyrics":{"action":"KEEP"},"artwork":"KEEP"}
                    """.formatted(format);
            JsonNode preview = mapper.readTree(mvc.perform(post("/api/songs/{id}/preview", resourceId)
                            .contentType(MediaType.APPLICATION_JSON).content(edit))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertTrue(preview.get("changes").toString().contains("title"));

            JsonNode processed = mapper.readTree(mvc.perform(post("/api/songs/{id}/process", resourceId)
                            .contentType(MediaType.APPLICATION_JSON).content(edit))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals("SUCCEEDED", processed.get("status").asText());
            long versionId = processed.get("versionId").asLong();
            byte[] output = mvc.perform(get("/api/versions/{id}/download", versionId))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            assertTrue(output.length > 0);

            byte[] report = mvc.perform(get(processed.get("reportUrl").asText()))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            JsonNode reportJson = mapper.readTree(report);
            assertEquals("SUCCEEDED", reportJson.get("status").asText());
            assertTrue(reportJson.has("sha256"));
            assertFalse(new String(report).contains("/Users/"));
            writeArtifact(format, input, output, report);
        }
    }

    @Test
    void previewsWavWithExplicitUnsupportedOperations() throws Exception {
        byte[] input = Files.readAllBytes(samples.resolve("source.wav"));
        JsonNode uploaded = mapper.readTree(mvc.perform(multipart("/api/songs")
                        .file(new MockMultipartFile("audio", "source.wav", "audio/wav", input)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        long resourceId = uploaded.get("resourceId").asLong();
        String edit = "{\"title\":{\"action\":\"SET\",\"value\":\"WAV title\"},\"lyrics\":{\"action\":\"SET\",\"value\":\"[00:01.00]unsupported\"},\"artwork\":\"SET\"}";
        mvc.perform(post("/api/songs/{id}/preview", resourceId).contentType(MediaType.APPLICATION_JSON).content(edit))
                .andExpect(status().isUnprocessableEntity());
    }

    private static void writeArtifact(String format, byte[] original, byte[] processed, byte[] report) throws Exception {
        String configured = System.getProperty("phase01.artifacts.dir");
        if (configured == null || configured.isBlank()) return;
        Path root = Path.of(configured);
        Files.createDirectories(root.resolve("original"));
        Files.createDirectories(root.resolve("processed"));
        Files.createDirectories(root.resolve("reports"));
        Files.write(root.resolve("original/source." + format), original);
        Files.write(root.resolve("processed/processed." + format), processed);
        Files.write(root.resolve("reports/processed-" + format + ".json"), report);
    }

    private static void runFfmpeg(List<String> args) throws Exception {
        var command = new java.util.ArrayList<String>();
        command.add("ffmpeg"); command.add("-hide_banner"); command.add("-loglevel"); command.add("error"); command.add("-y"); command.addAll(args);
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes());
        assertEquals(0, p.waitFor(), output);
    }
}

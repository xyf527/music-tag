package com.xin.musictag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.application.SingleSongService;
import com.xin.musictag.tagging.AudioMetadata;
import com.xin.musictag.tagging.AudioTagHandlerRegistry;
import com.xin.musictag.domain.StorageSettings;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
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
    @Autowired JdbcTemplate jdbc;
    @Autowired SingleSongService songs;
    @Autowired AudioTagHandlerRegistry handlers;
    @Autowired StorageSettings storage;
    @TempDir static Path samples;

    @BeforeAll
    static void makeSamples() throws Exception {
        runFfmpeg(List.of("-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-ar", "44100", "-ac", "1", samples.resolve("source.wav").toString()));
        runFfmpeg(List.of("-i", samples.resolve("source.wav").toString(), samples.resolve("source.mp3").toString()));
        runFfmpeg(List.of("-i", samples.resolve("source.wav").toString(), samples.resolve("source.flac").toString()));
        BufferedImage cover = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) cover.setRGB(x, y, Color.BLUE.getRGB());
        ImageIO.write(cover, "png", samples.resolve("cover.png").toFile());
    }

    @Test
    void uploadsPreviewsProcessesAndDownloadsMp3AndFlac() throws Exception {
        for (String format : List.of("mp3", "flac")) {
            byte[] input = Files.readAllBytes(samples.resolve("source." + format));
            byte[] cover = Files.readAllBytes(samples.resolve("cover.png"));
            JsonNode uploaded = mapper.readTree(mvc.perform(multipart("/api/songs")
                            .file(new MockMultipartFile("audio", "source." + format, "audio/" + format, input))
                            .file(new MockMultipartFile("lyrics", "source.lrc", "text/plain", "[00:00.00] Uploaded lyric".getBytes()))
                            .file(new MockMultipartFile("cover", "cover.png", "image/png", cover)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            long resourceId = uploaded.get("resourceId").asLong();

            String edit = ("{\"title\":{\"action\":\"SET\",\"value\":\"Phase 01 %s\"},\"artist\":{\"action\":\"SET\",\"value\":\"Artist 01\"},\"album\":{\"action\":\"SET\",\"value\":\"Album 01\"},\"lyrics\":{\"action\":\"KEEP\"},\"artwork\":\"KEEP\"}").formatted(format);
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
            assertFalse(new String(report).contains(System.getProperty("user.home")));
            AudioMetadata written = handlers.require(format).read(songs.version(versionId).outputPath());
            assertEquals("Phase 01 " + format, written.title());
            assertEquals("Artist 01", written.artist());
            assertEquals("Album 01", written.album());
            assertEquals("[00:00.00] Uploaded lyric", written.lyrics());
            assertTrue(written.artworkPresent());
            assertNotEquals(sha256(input), sha256(output));
            JsonNode taskView = mapper.readTree(mvc.perform(get("/api/tasks/{id}", processed.get("taskId").asLong()))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertFalse(taskView.has("reportPath"));
            assertFalse(taskView.toString().contains("storagePath"));
            assertFalse(taskView.toString().contains("outputPath"));

            String secondEdit = "{\"title\":{\"action\":\"KEEP\"},\"artist\":{\"action\":\"SET\",\"value\":\"Artist 02\"},\"album\":{\"action\":\"KEEP\"},\"lyrics\":{\"action\":\"KEEP\"},\"artwork\":\"KEEP\"}";
            JsonNode second = mapper.readTree(mvc.perform(post("/api/songs/{id}/process", resourceId)
                            .contentType(MediaType.APPLICATION_JSON).content(secondEdit))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals("SUCCEEDED", second.get("status").asText());
            long secondVersionId = second.get("versionId").asLong();
            Long parent = jdbc.queryForObject("select parent_version_id from music_version where id=?", Long.class, secondVersionId);
            assertEquals(versionId, parent);
            AudioMetadata secondWritten = handlers.require(format).read(songs.version(secondVersionId).outputPath());
            assertEquals("Phase 01 " + format, secondWritten.title());
            assertEquals("Artist 02", secondWritten.artist());
            assertEquals("[00:00.00] Uploaded lyric", secondWritten.lyrics());
            assertTrue(secondWritten.artworkPresent());
            String thirdEdit = "{\"title\":{\"action\":\"KEEP\"},\"artist\":{\"action\":\"KEEP\"},\"album\":{\"action\":\"KEEP\"},\"lyrics\":{\"action\":\"REMOVE\"},\"artwork\":\"REMOVE\"}";
            JsonNode third = mapper.readTree(mvc.perform(post("/api/songs/{id}/process", resourceId)
                            .contentType(MediaType.APPLICATION_JSON).content(thirdEdit))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
            assertEquals("SUCCEEDED", third.get("status").asText());
            Long secondParent = jdbc.queryForObject("select parent_version_id from music_version where id=?", Long.class, third.get("versionId").asLong());
            assertEquals(secondVersionId, secondParent);
            AudioMetadata thirdWritten = handlers.require(format).read(songs.version(third.get("versionId").asLong()).outputPath());
            assertNull(thirdWritten.lyrics());
            assertFalse(thirdWritten.artworkPresent());
            Path outside = samples.resolve("outside.mp3");
            Files.write(outside, input);
            Path versionPath = songs.version(versionId).outputPath();
            jdbc.update("update music_version set output_path=? where id=?", storage.outputs().resolve("..").resolve("outside.mp3").toString(), versionId);
            mvc.perform(get("/api/versions/{id}/download", versionId)).andExpect(status().isUnprocessableEntity());
            Path link = versionPath.getParent().resolve("escape.mp3");
            Files.createSymbolicLink(link, outside);
            jdbc.update("update music_version set output_path=? where id=?", link.toString(), versionId);
            mvc.perform(get("/api/versions/{id}/download", versionId)).andExpect(status().isUnprocessableEntity());
            Path outsideReport = samples.resolve("outside-report.json");
            Files.writeString(outsideReport, "{}");
            jdbc.update("update processing_task set report_path=? where id=?", storage.reports().resolve("..").resolve("outside-report.json").toString(), processed.get("taskId").asLong());
            mvc.perform(get("/api/tasks/{id}/report", processed.get("taskId").asLong())).andExpect(status().isUnprocessableEntity());
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

    @Test
    void rejectsUnsafeOrMismatchedUploadsAndInvalidDownloadIdentifiers() throws Exception {
        byte[] mp3 = Files.readAllBytes(samples.resolve("source.mp3"));
        String emptyMessage = mvc.perform(multipart("/api/songs").file(new MockMultipartFile("audio", "", "audio/mpeg", new byte[0])))
                .andExpect(status().isUnprocessableEntity()).andReturn().getResponse().getContentAsString();
        assertTrue(emptyMessage.contains("音频文件为空"));
        mvc.perform(multipart("/api/songs").file(new MockMultipartFile("audio", "wrong.mp3", "audio/mpeg", Files.readAllBytes(samples.resolve("source.wav")))))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(multipart("/api/songs").file(new MockMultipartFile("audio", "../escape.mp3", "audio/mpeg", mp3)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/versions/not-a-number/download"))
                .andExpect(status().isBadRequest());
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

    private static String sha256(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}

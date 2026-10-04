package com.xin.musictag;

import com.xin.musictag.application.OperationsSettings;
import com.xin.musictag.domain.StorageSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"music.database.enabled=false", "music.operations.minio-enabled=true",
        "spring.datasource.url=jdbc:invalid:must-not-connect", "spring.datasource.driver-class-name=invalid.Driver",
        "music.storage.root=.test-data/must-not-use"})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class MemoryModeTest {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired OperationsSettings operations;
    @Autowired StorageSettings storage;
    @Autowired org.springframework.context.ApplicationContext context;
    @Autowired com.xin.musictag.application.AudioFileService uploads;
    @Autowired com.xin.musictag.application.SingleSongService songs;
    @Autowired com.xin.musictag.application.StorageOperations backup;
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path samples;

    @Test void disabledDatabaseUsesMemoryAndSuppressesBackup() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertEquals("H2", connection.getMetaData().getDatabaseProductName());
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
        assertFalse(operations.minioEnabled());
        assertTrue(context.getBeansOfType(org.flywaydb.core.Flyway.class).isEmpty());
        assertTrue(storage.root().getFileName().toString().startsWith("music-tag-ephemeral-"));
        assertNotNull(jdbc.queryForObject("select count(*) from backup_job", Integer.class));
    }

    @Test void memoryModeStillProcessesAndDownloadsWithoutBackup() throws Exception {
        var audio = Phase04Fixtures.generate(samples, "mp3");
        var resource = uploads.upload(new org.springframework.mock.web.MockMultipartFile(
                "audio", "sample.mp3", "audio/mpeg", java.nio.file.Files.readAllBytes(audio)), null, null);
        var keep = new com.xin.musictag.domain.TagEditPlan(
                com.xin.musictag.tagging.FieldChange.keep(), com.xin.musictag.tagging.FieldChange.keep(),
                com.xin.musictag.tagging.FieldChange.keep(), com.xin.musictag.tagging.FieldChange.keep(),
                com.xin.musictag.tagging.UpdateAction.KEEP);
        var result = songs.process(resource.id(), keep);
        assertEquals("SUCCEEDED", result.status());
        assertTrue(java.nio.file.Files.size(songs.version(result.versionId()).outputPath()) > 0);
        assertTrue(backup.status(result.taskId()).toString().contains("DISABLED"));
    }

    @Test void wavOutputUsesVerifiedChineseTitleAndArtistWithoutChangingAudio() throws Exception {
        var audio = Phase04Fixtures.generate(samples, "wav");
        var before = Phase04Fixtures.pcm(audio);
        var resource = uploads.upload(new org.springframework.mock.web.MockMultipartFile(
                "audio", "original.wav", "audio/wav", java.nio.file.Files.readAllBytes(audio)), null, null);
        var plan = new com.xin.musictag.domain.TagEditPlan(
                com.xin.musictag.tagging.FieldChange.set("我的歌曲"), com.xin.musictag.tagging.FieldChange.set("我的歌手"),
                com.xin.musictag.tagging.FieldChange.keep(), com.xin.musictag.tagging.FieldChange.keep(),
                com.xin.musictag.tagging.UpdateAction.KEEP);
        var result = songs.process(resource.id(), plan);
        assertEquals("SUCCEEDED", result.status());
        var version = songs.version(result.versionId());
        assertEquals("我的歌曲 - 我的歌手.wav", songs.outputFilename(version));
        assertArrayEquals(before, Phase04Fixtures.pcm(version.outputPath()));
        var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(result.downloadUrl()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn().getResponse();
        assertEquals("我的歌曲 - 我的歌手.wav", org.springframework.http.ContentDisposition.parse(
                response.getHeader("Content-Disposition")).getFilename());
        assertArrayEquals(java.nio.file.Files.readAllBytes(version.outputPath()), response.getContentAsByteArray());
    }
    @Test void wavLyricsAndArtworkSetKeepRemovePreservePcmAndOriginal() throws Exception {
        var audio=Phase04Fixtures.generate(samples,"wav");var original=java.nio.file.Files.readAllBytes(audio);var pcm=Phase04Fixtures.pcm(audio);
        var cover=Phase04Fixtures.cover(samples);var coverBytes=java.nio.file.Files.readAllBytes(cover);
        var resource=uploads.upload(new org.springframework.mock.web.MockMultipartFile("audio","sample.wav","audio/wav",original),
                new org.springframework.mock.web.MockMultipartFile("lyrics","sample.lrc","text/plain","[00:00.00] 原创歌词".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                new org.springframework.mock.web.MockMultipartFile("cover","cover.png","image/png",coverBytes));
        var keep=com.xin.musictag.tagging.FieldChange.keep();var handler=new com.xin.musictag.tagging.WavTagHandler();
        var plan=new com.xin.musictag.domain.TagEditPlan(keep,keep,keep,keep,com.xin.musictag.tagging.UpdateAction.KEEP);
        var first=songs.process(resource.id(),plan);assertEquals("SUCCEEDED",first.status());
        var v1=songs.version(first.versionId());var m1=handler.read(v1.outputPath());assertEquals("[00:00.00] 原创歌词",m1.lyrics());assertTrue(m1.artworkPresent());
        assertEquals(com.xin.musictag.application.StorageOperations.hash(coverBytes),m1.artworkSha256());
        var second=songs.process(resource.id(),plan);assertEquals("SUCCEEDED",second.status());var v2=songs.version(second.versionId());assertEquals(m1,handler.read(v2.outputPath()));
        var replacement=new com.xin.musictag.domain.TagEditPlan(keep,keep,keep,com.xin.musictag.tagging.FieldChange.set("[00:01.00] 修改后的歌词"),com.xin.musictag.tagging.UpdateAction.SET);
        var modified=songs.process(resource.id(),replacement);assertEquals("SUCCEEDED",modified.status());var vModified=songs.version(modified.versionId());assertEquals("[00:01.00] 修改后的歌词",handler.read(vModified.outputPath()).lyrics());assertEquals(m1.artworkSha256(),handler.read(vModified.outputPath()).artworkSha256());
        var remove=new com.xin.musictag.domain.TagEditPlan(keep,keep,keep,com.xin.musictag.tagging.FieldChange.remove(),com.xin.musictag.tagging.UpdateAction.REMOVE);
        var third=songs.process(resource.id(),remove);assertEquals("SUCCEEDED",third.status());var v3=songs.version(third.versionId());var m3=handler.read(v3.outputPath());assertNull(m3.lyrics());assertFalse(m3.artworkPresent());
        for(var v:java.util.List.of(v1,v2,vModified,v3))assertArrayEquals(pcm,Phase04Fixtures.pcm(v.outputPath()));
        assertArrayEquals(original,java.nio.file.Files.readAllBytes(resource.storagePath()));
    }
}

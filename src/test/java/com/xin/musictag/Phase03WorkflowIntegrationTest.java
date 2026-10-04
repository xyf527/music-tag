package com.xin.musictag;

import com.xin.musictag.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"music.storage.root=.test-data/phase03-workflow-${random.uuid}","spring.datasource.url=jdbc:h2:mem:phase03workflow;MODE=MySQL;DB_CLOSE_DELAY=-1","music.operations.minio-enabled=true"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class Phase03WorkflowIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired StorageOperations operations;
    @MockitoBean BackupStore remote; @MockitoBean DiskGuard disk;
    @TempDir Path samples;
    @Test void localProcessingSurvivesBackupOutageAndLowDiskAllowsDownload()throws Exception {
        Path audio=samples.resolve("fixture.mp3");var ffmpeg=new ProcessBuilder("ffmpeg","-hide_banner","-loglevel","error","-y","-f","lavfi","-i","sine=frequency=500:duration=1",audio.toString()).start();assertEquals(0,ffmpeg.waitFor());
        byte[] bytes=Files.readAllBytes(audio);
        long resource=mapper.readTree(mvc.perform(multipart("/api/songs").file(new MockMultipartFile("audio","fixture.mp3","audio/mpeg",bytes))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).get("resourceId").asLong();
        var result=mapper.readTree(mvc.perform(post("/api/songs/"+resource+"/process").contentType("application/json").content("{}" )).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertEquals("SUCCEEDED",result.get("status").asText());long task=result.get("taskId").asLong(),version=result.get("versionId").asLong();
        doThrow(new Exception("SECRET_REMOTE" )).when(remote).putFile(anyString(),any(Path.class),anyString());operations.retry(version);
        assertEquals("FAILED",((java.util.List<java.util.Map<String,Object>>)operations.status(task).get("backups")).get(0).get("status"));
        var state=mvc.perform(get("/api/tasks/"+task)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertTrue(state.contains("SUCCEEDED"));assertFalse(state.contains("SECRET_REMOTE"));
        doThrow(new com.xin.musictag.domain.ProcessingException("DISK_LOW","UPLOAD","磁盘空间不足，已暂停新上传")).when(disk).requireUpload();
        mvc.perform(multipart("/api/songs").file(new MockMultipartFile("audio","fixture.mp3","audio/mpeg",bytes))).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/versions/"+version+"/download")).andExpect(status().isOk());mvc.perform(get("/api/tasks/"+task)).andExpect(status().isOk());
        doNothing().when(remote).putFile(anyString(),any(Path.class),anyString());operations.retry(version);assertTrue(operations.status(task).toString().contains("SUCCEEDED"));
    }
}

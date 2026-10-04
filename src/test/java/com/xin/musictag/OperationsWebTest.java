package com.xin.musictag;

import com.xin.musictag.application.*;
import com.xin.musictag.domain.*;
import com.xin.musictag.web.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class OperationsWebTest {
    @TempDir Path root;
    @Test void readinessOmitsSecretsAndDoesNotCallDisabledMinio()throws Exception {
        var db=mock(JdbcTemplate.class);when(db.queryForObject("select 1",Integer.class)).thenReturn(1);
        when(db.queryForObject("select count(*) from flyway_schema_history where success=false",Integer.class)).thenReturn(0);
        var remote=mock(BackupStore.class);var settings=new OperationsSettings(false,null,"SECRET_ACCESS","SECRET_KEY",null,null,null,0,0);
        var controller=new OperationsController(mock(StorageOperations.class),db,new StorageSettings(root,1000),settings,remote,mock(DiskGuard.class));
        var mvc=MockMvcBuilders.standaloneSetup(controller).build();
        var response=mvc.perform(get("/health/ready")).andReturn().getResponse();assertEquals(200,response.getStatus());
        assertFalse(response.getContentAsString().contains("SECRET"));assertFalse(response.getContentAsString().contains(root.toString()));verifyNoInteractions(remote);
        when(db.queryForObject("select 1",Integer.class)).thenThrow(new RuntimeException("SECRET_CONNECTION"));
        response=mvc.perform(get("/health/ready")).andReturn().getResponse();assertEquals(503,response.getStatus());assertFalse(response.getContentAsString().contains("SECRET"));
        assertEquals(200,mvc.perform(get("/health/live")).andReturn().getResponse().getStatus());
    }
    @Test void diskGuardBlocksUploadButOutputReadStillWorks()throws Exception {
        var guard=mock(DiskGuard.class);doThrow(new ProcessingException("DISK_LOW","UPLOAD","磁盘空间不足")).when(guard).requireUpload();
        var files=new AudioFileService(new StorageSettings(root,1000),mock(UploadLimits.class),mock(ResourceRepository.class),new com.xin.musictag.tagging.AudioTagHandlerRegistry(java.util.List.of()));
        org.springframework.test.util.ReflectionTestUtils.setField(files,"diskGuard",guard);
        assertEquals("DISK_LOW",assertThrows(ProcessingException.class,()->files.upload(null,null,null)).code());
        Path output=root.resolve("outputs/version.mp3");Files.createDirectories(output.getParent());Files.writeString(output,"output");
        var songs=new SingleSongService(mock(ResourceRepository.class),mock(TaskRepository.class),mock(VersionRepository.class),new com.xin.musictag.tagging.AudioTagHandlerRegistry(java.util.List.of()),new StorageSettings(root,1000),new com.fasterxml.jackson.databind.ObjectMapper());
        assertEquals("output",new String(songs.outputBytes(new VersionRecord(1,1,null,1,output,"hash",java.time.Instant.now()))));verify(guard,times(1)).requireUpload();
    }
}

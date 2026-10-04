package com.xin.musictag;

import com.xin.musictag.application.*;
import com.xin.musictag.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import java.nio.file.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StorageOperationsTest {
    @TempDir Path root;
    JdbcTemplate db; StorageSettings storage; Instant instant=Instant.parse("2026-10-04T00:00:00Z");
    Map<String,byte[]> objects=new HashMap<>(); boolean fail; int writes;
    BackupStore remote=new BackupStore(){public boolean ready(){return !fail;} public void put(String key,byte[] content,String hash)throws Exception{if(fail)throw new Exception("SECRET_DO_NOT_EXPOSE");if(!objects.containsKey(key)){objects.put(key,content);writes++;}}};
    @BeforeEach void setup()throws Exception {
        String url="jdbc:h2:mem:operations_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").load().migrate();db=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
        storage=new StorageSettings(root,1000);for(Path p:List.of(storage.uploads(),storage.outputs(),storage.working(),storage.reports()))Files.createDirectories(p);
    }
    StorageOperations operations(boolean enabled,Instant time){return new StorageOperations(db,storage,new OperationsSettings(enabled,"https://invalid.example","fake-access","fake-secret","fake-bucket","isolated/music",Duration.ofDays(30),0,0),remote,Clock.fixed(time,ZoneOffset.UTC),new ObjectMapper());}
    Path seed(Instant created)throws Exception {
        Path original=storage.uploads().resolve("original"),output=storage.outputs().resolve("version");Files.writeString(original,"original");Files.writeString(output,"processed");
        db.update("insert into music_resource(id,original_filename,detected_format,byte_size,sha256,storage_path,created_at) values(1,'sample.mp3','mp3',8,'hash',?,?)",original.toString(),Timestamp.from(created));
        db.update("insert into processing_task(id,resource_id,status,stage,created_at) values(1,1,'SUCCEEDED','PUBLISHED',?)",Timestamp.from(created));
        db.update("insert into music_version(id,source_resource_id,task_id,output_path,sha256,created_at) values(1,1,1,?,?,?)",output.toString(),StorageOperations.hash(Files.readAllBytes(output)),Timestamp.from(created));return output;
    }
    @Test void disabledNeedsNoCredentialsAndDoesNotContactRemote()throws Exception {
        seed(instant);var ops=operations(false,instant);fail=true;ops.enqueue(1);ops.retryBackups();assertEquals("DISABLED",db.queryForObject("select status from backup_job",String.class));assertEquals(0,writes);
        assertTrue(new OperationsSettings(false,null,null,null,null,null,null,0,0).configured());
    }
    @Test void failureDoesNotRollbackLocalSuccessAndRetryIsStable()throws Exception {
        Path output=seed(instant);var ops=operations(true,instant);fail=true;ops.retry(1);
        assertEquals("SUCCEEDED",db.queryForObject("select status from processing_task",String.class));assertTrue(Files.exists(output));
        assertEquals("FAILED",db.queryForObject("select status from backup_job",String.class));assertNotNull(db.queryForObject("select next_retry from backup_job",Timestamp.class));
        assertTrue(ops.status(1).get("cleanupReason").toString().contains("暂缓"));
        fail=false;ops.retry(1);int first=writes;ops.retry(1);assertEquals(first,writes);assertEquals(2,writes);
        assertTrue(objects.keySet().stream().allMatch(k->k.startsWith("isolated/music/2026/10/1/version-1/")));
        String keys=db.queryForObject("select object_keys from backup_job",String.class);assertFalse(keys.contains("fake-secret"));assertTrue(keys.contains("manifest.json"));
    }
    @Test void boundaryDryRunAndBackupHold()throws Exception {
        Path output=seed(instant.minus(Duration.ofDays(30)));var ops=operations(false,instant.minusSeconds(1));assertEquals(0,ops.cleanup(false).get("deleted"));
        ops=operations(true,instant);ops.enqueue(1);assertTrue(ops.cleanup(false).get("skipped")>0);assertTrue(Files.exists(output));
        ops.retry(1);assertEquals(2,ops.cleanup(true).get("deleted"));assertTrue(Files.exists(output));assertEquals(2,ops.cleanup(false).get("deleted"));assertFalse(Files.exists(output));
        assertEquals("DELETED",db.queryForObject("select file_status from music_version",String.class));assertEquals(0,ops.cleanup(false).get("deleted"));
    }
    @Test void rejectsSymlinksAndOutsideRoots()throws Exception {
        Path output=seed(instant.minus(Duration.ofDays(31)));Path outside=Files.createTempFile("protected-",".txt");
        try{Files.delete(output);Files.createSymbolicLink(output,outside);assertThrows(Exception.class,()->StorageOperations.safe(output,storage.outputs()));assertThrows(Exception.class,()->StorageOperations.safe(outside,storage.outputs()));assertEquals(1,operations(false,instant).cleanup(false).get("failed"));assertTrue(Files.exists(outside));}finally{Files.deleteIfExists(outside);}
    }
    @Test void reconcilesPublishedVersionAndPreservesUncertainFilesIdempotently()throws Exception {
        seed(instant);db.update("update processing_task set status='RUNNING',output_version_id=null");
        Path orphan=storage.outputs().resolve("orphan.publishing"),work=storage.working().resolve("valuable");Files.writeString(orphan,"valuable");Files.writeString(work,"valuable");
        Path report=storage.reports().resolve("task-1.json");Files.writeString(report,"{}");
        var ops=operations(false,instant);ops.recover();ops.recover();assertEquals("SUCCEEDED",db.queryForObject("select status from processing_task",String.class));assertEquals(1L,db.queryForObject("select output_version_id from processing_task",Long.class));assertEquals(report.toString(),db.queryForObject("select report_path from processing_task",String.class));assertEquals(2,db.queryForObject("select count(*) from recovery_issue",Integer.class));assertTrue(Files.exists(work));assertTrue(Files.exists(orphan));
        Files.delete(storage.outputs().resolve("version"));ops.recover();assertEquals("MISSING",db.queryForObject("select file_status from music_version",String.class));
    }
    @Test void thresholdStopsAtBoundary(){assertFalse(DiskGuard.sufficient(100,1000,100,0));assertFalse(DiskGuard.sufficient(100,1000,0,10));assertTrue(DiskGuard.sufficient(101,1000,100,10));}
    @Test void journalRecoversFileBeforeDatabasePublication()throws Exception {
        Path output=seed(instant);db.update("delete from music_version");db.update("update processing_task set status='RUNNING'");
        var ops=operations(false,instant);ops.preparePublication(1,1,null,null,output,StorageOperations.hash(Files.readAllBytes(output)));ops.recover();ops.recover();
        assertEquals(1,db.queryForObject("select count(*) from music_version",Integer.class));assertEquals("SUCCEEDED",db.queryForObject("select status from processing_task",String.class));assertTrue(Files.exists(storage.reports().resolve("task-1.json")));assertFalse(Files.exists(storage.reports().resolve("task-1.publishing")));
    }
}

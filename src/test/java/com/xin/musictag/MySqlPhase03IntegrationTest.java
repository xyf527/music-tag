package com.xin.musictag;

import com.xin.musictag.application.*;
import com.xin.musictag.domain.StorageSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import java.nio.file.*;
import java.time.*;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="MYSQL_IT_URL",matches=".+")
class MySqlPhase03IntegrationTest {
    @TempDir Path root;
    @Test void mysqlMigrationBackupAndLifecycleRoundTrip()throws Exception {
        var source=new DriverManagerDataSource(System.getenv("MYSQL_IT_URL"),System.getenv("MYSQL_IT_USER"),System.getenv("MYSQL_IT_PASSWORD"));
        Flyway.configure().dataSource(source).load().migrate();var db=new JdbcTemplate(source);
        assertEquals("5",db.queryForObject("select version from flyway_schema_history where version='5' and success=1",String.class));
        var storage=new StorageSettings(root,1000);Files.createDirectories(storage.uploads());Files.createDirectories(storage.outputs());
        Path original=storage.uploads().resolve("original"),output=storage.outputs().resolve("processed");Files.writeString(original,"fixture");Files.writeString(output,"verified");
        Instant time=Instant.parse("2026-10-04T00:00:00Z");Timestamp old=Timestamp.from(time.minus(Duration.ofDays(30)));
        db.update("insert into music_resource(original_filename,detected_format,byte_size,sha256,storage_path,created_at) values('phase03.mp3','mp3',7,'fake',?,?)",original.toString(),old);
        long resource=db.queryForObject("select id from music_resource where storage_path=?",Long.class,original.toString());
        db.update("insert into processing_task(resource_id,status,stage,created_at) values(?,'SUCCEEDED','PUBLISHED',?)",resource,old);
        long task=db.queryForObject("select id from processing_task where resource_id=?",Long.class,resource);
        db.update("insert into music_version(source_resource_id,task_id,output_path,sha256,created_at) values(?,?,?,?,?)",resource,task,output.toString(),StorageOperations.hash(Files.readAllBytes(output)),old);
        long version=db.queryForObject("select id from music_version where task_id=?",Long.class,task);
        try {
            var remote=new BackupStore(){public boolean ready(){return true;}public void put(String key,byte[] bytes,String hash){assertTrue(key.startsWith("phase03-test/"));}};
            var ops=new StorageOperations(db,storage,new OperationsSettings(true,"https://invalid.example","FAKE","FAKE","fake","phase03-test",Duration.ofDays(30),0,0),remote,Clock.fixed(time,ZoneOffset.UTC),new ObjectMapper());
            ops.retry(version);assertEquals("SUCCEEDED",db.queryForObject("select status from backup_job where version_id=?",String.class,version));
            ops.cleanup(false);assertEquals("DELETED",db.queryForObject("select file_status from music_version where id=?",String.class,version));assertFalse(Files.exists(output));
        } finally {db.update("delete from backup_job where version_id=?",version);db.update("delete from music_version where id=?",version);db.update("delete from processing_task where id=?",task);db.update("delete from music_resource where id=?",resource);}
    }
}

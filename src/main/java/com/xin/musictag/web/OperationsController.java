package com.xin.musictag.web;

import com.xin.musictag.application.*;
import com.xin.musictag.domain.StorageSettings;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Files;
import java.util.*;

@RestController
public class OperationsController {
    private final StorageOperations operations; private final JdbcTemplate db; private final StorageSettings storage;
    private final OperationsSettings settings; private final BackupStore remote; private final DiskGuard disk;
    @Value("${music.build.commit:UNKNOWN}") private String commit;
    @Value("${music.build.time:UNKNOWN}") private String buildTime;
    @Value("${music.build.version:UNKNOWN}") private String version;
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.beans.factory.ObjectProvider<org.springframework.boot.info.BuildProperties> build;
    public OperationsController(StorageOperations operations,JdbcTemplate db,StorageSettings storage,OperationsSettings settings,BackupStore remote,DiskGuard disk){this.operations=operations;this.db=db;this.storage=storage;this.settings=settings;this.remote=remote;this.disk=disk;}
    @GetMapping("/health/live") public Map<String,String> live(){return Map.of("status","UP");}
    @GetMapping("/health/ready") public ResponseEntity<Map<String,Object>> ready(){
        boolean database=false, directories=true;
        try{database=db.queryForObject("select 1",Integer.class)==1 && db.queryForObject("select count(*) from flyway_schema_history where success=false",Integer.class)==0; }catch(Exception ignored){database=false;}
        for(var p:List.of(storage.uploads(),storage.working(),storage.outputs(),storage.reports())) try{Files.createDirectories(p);var probe=Files.createTempFile(p,"health-",".tmp");Files.delete(probe);}catch(Exception e){directories=false;}
        boolean backup=settings.configured()&&(!settings.minioEnabled()||remote.ready()); boolean ok=database&&directories&&backup;
        return ResponseEntity.status(ok?200:503).body(Map.of("status",ok?"UP":"DOWN","database",database,"storage",directories,"backup",backup));
    }
    @GetMapping("/api/version") public Map<String,String> version(){var info=build==null?null:build.getIfAvailable();return Map.of("commit",commit,"buildTime","UNKNOWN".equals(buildTime)&&info!=null?info.getTime().toString():buildTime,"version","UNKNOWN".equals(version)&&info!=null?info.getVersion():version);}
    @GetMapping("/api/maintenance/history") public List<Map<String,Object>> history(){return db.queryForList("select dry_run,deleted_count,skipped_count,failed_count,created_at from cleanup_history order by id desc limit 100");}
    @GetMapping("/api/operations") public Map<String,Object> status(){return Map.of("uploadAllowed",disk.available(),"warning",disk.available()?"":"磁盘空间不足，已暂停新上传；查询和下载仍可使用","recoveryIssues",db.queryForList("select issue_key,error_code,created_at from recovery_issue"));}
    @GetMapping("/api/tasks/{id}/backup") public Map<String,Object> backup(@PathVariable long id){return operations.status(id);}
    @GetMapping("/api/batches/{id}/backup") public List<Map<String,Object>> batchBackup(@PathVariable long id){return db.queryForList("select i.id,b.status,b.error_code,b.attempts,v.file_status from batch_item i join music_version v on v.id=i.output_version_id left join backup_job b on b.version_id=v.id where i.batch_task_id=?",id);}
    @PostMapping("/api/versions/{id}/backup/retry") public Map<String,String> retry(@PathVariable long id){operations.retry(id);return Map.of("status","REQUESTED");}
    @PostMapping("/api/maintenance/cleanup") public Map<String,Integer> cleanup(@RequestParam(defaultValue="true") boolean dryRun){return operations.cleanup(dryRun);}
}

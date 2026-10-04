package com.xin.musictag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.domain.StorageSettings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.nio.file.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import java.security.MessageDigest;

/** Single-instance maintenance. All remote errors are reduced to safe codes. */
@Service
public class StorageOperations {
    private final JdbcTemplate db; private final StorageSettings storage; private final OperationsSettings settings;
    private final BackupStore remote; private final Clock clock; private final ObjectMapper mapper;
    private volatile boolean recovered;
    public StorageOperations(JdbcTemplate db, StorageSettings storage, OperationsSettings settings, BackupStore remote, Clock clock, ObjectMapper mapper) {
        this.db=db; this.storage=storage; this.settings=settings; this.remote=remote; this.clock=clock; this.mapper=mapper;
    }
    public synchronized void enqueue(long version) {
        if (db.queryForObject("select count(*) from backup_job where version_id=?", Integer.class, version)==0)
            db.update("insert into backup_job(version_id,status,updated_at) values(?,?,?)", version, settings.minioEnabled()?"PENDING":"DISABLED", now());
    }
    private Timestamp now() { return Timestamp.from(clock.instant()); }
    public void preparePublication(long task,long resource,Long parent,Long item,Path output,String hash)throws Exception {
        preparePublication(task,resource,parent,item,output,hash,Map.of());
    }
    public void preparePublication(long task,long resource,Long parent,Long item,Path output,String hash,Map<String,Object> details)throws Exception {
        Files.createDirectories(storage.reports());
        var doc=new LinkedHashMap<String,Object>();doc.put("task",task);doc.put("resource",resource);doc.put("parent",parent);doc.put("item",item);doc.put("output",output.toString());doc.put("hash",hash);
        doc.put("details",details);
        Path marker=storage.reports().resolve("task-"+task+".publishing"),temp=storage.reports().resolve("task-"+task+".journal.tmp");
        mapper.writeValue(temp.toFile(),doc);Files.move(temp,marker,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    public void finishPublication(long task){try{Files.deleteIfExists(storage.reports().resolve("task-"+task+".publishing"));}catch(Exception ignored){}}
    private void recoverPublication(Path marker)throws Exception {
        safe(marker,storage.reports());var doc=mapper.readTree(marker.toFile());long task=doc.get("task").asLong();
        Path output=safe(Path.of(doc.get("output").asText()),storage.outputs());String hash=AudioFileService.sha256(output);
        if(!hash.equals(doc.get("hash").asText())) throw new IllegalArgumentException("HASH_MISMATCH");
        if(db.queryForObject("select count(*) from music_version where task_id=?",Integer.class,task)==0)
            db.update("insert into music_version(source_resource_id,parent_version_id,task_id,output_path,sha256,created_at,batch_item_id) values(?,?,?,?,?,?,?)",doc.get("resource").asLong(),doc.get("parent").isNull()?null:doc.get("parent").asLong(),task,output.toString(),hash,Timestamp.from(Files.getLastModifiedTime(marker).toInstant()),doc.get("item").isNull()?null:doc.get("item").asLong());
        long version=db.queryForObject("select id from music_version where task_id=?",Long.class,task);
        db.update("update processing_task set output_version_id=?,status='SUCCEEDED',stage='PUBLISHED',error_code=null,error_message=null where id=?",version,task);
        if(!doc.get("item").isNull()) db.update("update batch_item set output_version_id=?,status='SUCCEEDED',stage='PUBLISHED',error_code=null where id=?",version,doc.get("item").asLong());
        Path report=storage.reports().resolve("task-"+task+".json");
        if(!Files.exists(report)){var recoveredReport=recoveredReport(task,version,hash);if(doc.has("details"))doc.get("details").fields().forEachRemaining(field->recoveredReport.put(field.getKey(),field.getValue()));mapper.writeValue(report.toFile(),recoveredReport);}
        db.update("update processing_task set report_path=? where id=?",report.toString(),task);enqueue(version);Files.delete(marker);
    }
    @Scheduled(fixedDelayString="${music.operations.backup-delay-ms:60000}") public void scheduledBackup(){if(recovered)retryBackups();}
    public synchronized void retryBackups() {
        if (!settings.minioEnabled()) return;
        db.queryForList("select v.id from music_version v join processing_task t on t.id=v.task_id where v.file_status='AVAILABLE' and t.status='SUCCEEDED'").forEach(r -> enqueue(((Number)r.get("id")).longValue()));
        db.update("update backup_job set status='PENDING' where status='DISABLED'");
        for (var job: db.queryForList("select version_id from backup_job where status in ('PENDING','FAILED') and (next_retry is null or next_retry<=?)", now())) backup(((Number)job.get("version_id")).longValue());
    }
    public synchronized void retry(long version) {
        enqueue(version);
        db.update("update backup_job set next_retry=null where version_id=?",version);
        if(settings.minioEnabled()) backup(version);
    }
    private void backup(long version) {
        if ("SUCCEEDED".equals(db.queryForObject("select status from backup_job where version_id=?",String.class,version))) return;
        db.update("update backup_job set status='RUNNING',attempts=attempts+1,updated_at=? where version_id=?",now(),version);
        var entries=new ArrayList<Map<String,Object>>();
        try {
            var row=db.queryForMap("select v.*,r.lyrics_path,r.cover_path from music_version v join music_resource r on r.id=v.source_resource_id where v.id=?",version);
            long task=((Number)row.get("task_id")).longValue();
            String base=settings.prefix()+"/"+java.time.format.DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC).format(((Timestamp)row.get("created_at")).toInstant())+"/"+task+"/version-"+version+"/";
            if(!AudioFileService.sha256(safe(Path.of(row.get("output_path").toString()),storage.outputs())).equals(row.get("sha256"))) throw new IllegalArgumentException("HASH_MISMATCH");
            upload(entries,base,"processed",row.get("output_path"),storage.outputs());
            upload(entries,base,"lyrics",row.get("lyrics_path"),storage.uploads());
            upload(entries,base,"covers",row.get("cover_path"),storage.uploads());
            // Batch cover/lyrics are represented by the verified embedded output as well as the batch attachment references.
            var attachments=db.queryForList("select a.storage_path,asset.kind from batch_item i join batch_item asset on asset.id=i.lyrics_item_id or asset.id=i.cover_item_id join batch_attachment a on a.item_id=asset.id where i.output_version_id=?",version);
            for(var a:attachments) upload(entries,base,"LYRICS".equals(a.get("kind"))?"lyrics":"covers",a.get("storage_path"),storage.uploads());
            byte[] manifest=mapper.writeValueAsBytes(Map.of("versionId",version,"taskId",task,"resourceId",row.get("source_resource_id"),"objects",entries));
            String manifestKey=base+"manifest.json"; remote.put(manifestKey,manifest,hash(manifest));
            db.update("update backup_job set status='SUCCEEDED',error_code=null,next_retry=null,object_keys=?,updated_at=? where version_id=?",mapper.writeValueAsString(Map.of("manifest",manifestKey,"objects",entries)),now(),version);
        } catch(Exception e) {
            int attempts=db.queryForObject("select attempts from backup_job where version_id=?",Integer.class,version);
            String keys="[]";try{keys=mapper.writeValueAsString(entries);}catch(Exception ignored){}
            db.update("update backup_job set status='FAILED',error_code='BACKUP_UNAVAILABLE',object_keys=?,next_retry=?,updated_at=? where version_id=?",keys,Timestamp.from(clock.instant().plusSeconds(Math.min(86400,60L*(1L<<Math.min(attempts,10))))),now(),version);
        }
    }
    private void upload(List<Map<String,Object>> entries,String base,String kind,Object value,Path root) throws Exception {
        if(value==null) return;
        Path file=safe(Path.of(value.toString()),root); String hash=AudioFileService.sha256(file);
        String key=base+kind+"/"+hash; remote.putFile(key,file,hash); entries.add(Map.of("key",key,"sha256",hash,"bytes",Files.size(file)));
    }
    public static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    public static Path safe(Path file,Path root) throws Exception {
        Path normalized=file.toAbsolutePath().normalize(), boundary=root.toAbsolutePath().normalize();
        if(!normalized.startsWith(boundary)) throw new IllegalArgumentException("UNSAFE_PATH");
        for(Path p=normalized;p!=null && p.startsWith(boundary);p=p.getParent()) if(Files.isSymbolicLink(p)) throw new IllegalArgumentException("UNSAFE_PATH");
        if(!Files.isRegularFile(normalized,LinkOption.NOFOLLOW_LINKS) || !normalized.toRealPath().startsWith(root.toRealPath())) throw new IllegalArgumentException("FILE_UNAVAILABLE");
        return normalized;
    }
    public Map<String,Object> status(long task) {
        var rows=db.queryForList("select b.status,b.attempts,b.error_code,b.next_retry,b.object_keys,v.file_status from backup_job b join music_version v on v.id=b.version_id where v.task_id=?",task);
        return Map.of("backups",rows,"cleanupReason",rows.stream().anyMatch(r->settings.minioEnabled()&&!"SUCCEEDED".equals(r.get("status")))?"备份未成功，暂缓本地清理":"");
    }
    public boolean downloadable(long version) {
        try {var v=db.queryForMap("select file_status,output_path from music_version where id=?",version);safe(Path.of(v.get("output_path").toString()),storage.outputs());return "AVAILABLE".equals(v.get("file_status"));}catch(Exception e){return false;}
    }
    @Scheduled(fixedDelayString="${music.operations.cleanup-delay-ms:3600000}") public void scheduledCleanup(){ if(recovered)cleanup(false); }
    public synchronized Map<String,Integer> cleanup(boolean dryRun) {
        int[] counts={0,0,0}; Timestamp cutoff=Timestamp.from(clock.instant().minus(settings.retention()));
        for(var v:db.queryForList("select * from music_version where created_at<=? and file_status='AVAILABLE'",cutoff)) {
            long id=((Number)v.get("id")).longValue();
            if(db.queryForObject("select count(*) from processing_task where id=? and status in ('PENDING','RUNNING','NEEDS_REVIEW')",Integer.class,v.get("task_id"))>0){counts[1]++;continue;}
            if(settings.minioEnabled()&&!backedUp(id)){counts[1]++;continue;}
            remove(v.get("output_path"),storage.outputs(),dryRun,counts,()->db.update("update music_version set file_status='DELETED' where id=?",id));
        }
        for(var r:db.queryForList("select * from music_resource where created_at<=? and file_status='AVAILABLE'",cutoff)) {
            long id=((Number)r.get("id")).longValue();
            int active=db.queryForObject("select count(*) from processing_task where resource_id=? and status in ('PENDING','RUNNING')",Integer.class,id);
            int pending=settings.minioEnabled()?db.queryForObject("select count(*) from music_version v left join backup_job b on b.version_id=v.id where v.source_resource_id=? and (b.status is null or b.status<>'SUCCEEDED')",Integer.class,id):0;
            if(active>0||pending>0){counts[1]++;continue;}
            boolean ok=true;
            for(String field:List.of("storage_path","lyrics_path","cover_path")) if(r.get(field)!=null) ok &= remove(r.get(field),storage.uploads(),dryRun,counts,()->{});
            if(ok&&!dryRun) db.update("update music_resource set file_status='DELETED' where id=?",id);
        }
        for(var a:db.queryForList("select a.storage_path,i.id,i.batch_task_id from batch_attachment a join batch_item i on i.id=a.item_id where i.created_at<=? and a.file_status='AVAILABLE'",cutoff)) {
            long batch=((Number)a.get("batch_task_id")).longValue();
            int active=db.queryForObject("select count(*) from batch_item where batch_task_id=? and kind='AUDIO' and status in ('PENDING','RUNNING','INTERRUPTED')",Integer.class,batch);
            int pending=settings.minioEnabled()?db.queryForObject("select count(*) from batch_item i join music_version v on v.id=i.output_version_id left join backup_job b on b.version_id=v.id where i.batch_task_id=? and (b.status is null or b.status<>'SUCCEEDED')",Integer.class,batch):0;
            if(active>0||pending>0){counts[1]++;continue;}
            remove(a.get("storage_path"),storage.uploads(),dryRun,counts,()->db.update("update batch_attachment set file_status='DELETED' where item_id=?",a.get("id")));
        }
        db.update("insert into cleanup_history(dry_run,deleted_count,skipped_count,failed_count,created_at) values(?,?,?,?,?)",dryRun,counts[0],counts[1],counts[2],now());
        return Map.of("deleted",counts[0],"skipped",counts[1],"failed",counts[2]);
    }
    private boolean backedUp(long id){return db.queryForObject("select count(*) from backup_job where version_id=? and status='SUCCEEDED'",Integer.class,id)>0;}
    private boolean remove(Object value,Path root,boolean dry,int[] counts,Runnable update){
        try {Path file=Path.of(value.toString());Path normalized=file.toAbsolutePath().normalize(),boundary=root.toAbsolutePath().normalize();
            if(!normalized.startsWith(boundary))throw new IllegalArgumentException("UNSAFE_PATH");
            for(Path p=normalized;p!=null&&p.startsWith(boundary);p=p.getParent())if(Files.isSymbolicLink(p))throw new IllegalArgumentException("UNSAFE_PATH");
            if(!Files.exists(file,LinkOption.NOFOLLOW_LINKS)){if(!dry)update.run();counts[1]++;return true;} safe(file,root);if(!dry){Files.delete(file);update.run();}counts[0]++;return true;}
        catch(Exception e){counts[2]++;return false;}
    }
    @EventListener(ApplicationReadyEvent.class) public synchronized void recover() {
        try{Files.createDirectories(storage.reports());try(var files=Files.list(storage.reports())){for(Path marker:files.filter(p->p.getFileName().toString().endsWith(".publishing")).toList())try{recoverPublication(marker);}catch(Exception e){issue(marker,"PUBLICATION_REQUIRES_REVIEW");}}}catch(Exception e){issue(storage.reports(),"SCAN_FAILED");}
        db.update("update backup_job set status='PENDING' where status='RUNNING'");
        for(var v:db.queryForList("select * from music_version")) {
            long id=((Number)v.get("id")).longValue(),task=((Number)v.get("task_id")).longValue();
            try { Path file=safe(Path.of(v.get("output_path").toString()),storage.outputs());
                if(!AudioFileService.sha256(file).equals(v.get("sha256"))) throw new IllegalArgumentException();
                db.update("update processing_task set status='SUCCEEDED',stage='PUBLISHED',output_version_id=?,error_code=null,error_message=null where id=?",id,task); enqueue(id);
                db.update("update music_version set file_status='AVAILABLE' where id=? and file_status='MISSING'",id);
                Path report=storage.reports().resolve("task-"+task+".json");
                try{if(!Files.exists(report)){Files.createDirectories(storage.reports());mapper.writeValue(report.toFile(),recoveredReport(task,id,v.get("sha256").toString()));}
                    safe(report,storage.reports());db.update("update processing_task set report_path=? where id=?",report.toString(),task);
                }catch(Exception e){issue(report,"REPORT_REQUIRES_REVIEW");}
            }catch(Exception e){db.update("update music_version set file_status='MISSING' where id=? and file_status<>'DELETED'",id);}
        }
        db.update("update processing_task set status='NEEDS_REVIEW',stage='RECOVERY',error_code='INTERRUPTED',error_message='中断任务需要人工检查' where status in ('PENDING','RUNNING')");
        for(var r:db.queryForList("select id,storage_path from music_resource where file_status='AVAILABLE'"))try{safe(Path.of(r.get("storage_path").toString()),storage.uploads());}catch(Exception e){db.update("update music_resource set file_status='MISSING' where id=?",r.get("id"));}
        cleanRecoveredWork();
        for(Path root:List.of(storage.outputs(),storage.working(),storage.reports())) {
            try {Files.createDirectories(root);try(var files=Files.walk(root)){for(Path file:files.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).toList()){
                boolean known=db.queryForObject("select count(*) from music_version where output_path=?",Integer.class,file.toString())>0;
                if(root.equals(storage.outputs())&&!known || root.equals(storage.working()) || file.getFileName().toString().endsWith(".publishing")) issue(file,"ORPHAN_REQUIRES_REVIEW");
                if(root.equals(storage.reports())&&file.getFileName().toString().matches("task-[0-9]+\\.json")) {
                    long task=Long.parseLong(file.getFileName().toString().replace("task-","").replace(".json",""));
                    db.update("update processing_task set report_path=? where id=? and report_path is null",file.toString(),task);
                }
            }}}catch(Exception e){issue(root,"SCAN_FAILED");}
        }
        recovered=true;
    }
    private void cleanRecoveredWork() {
        try {Files.createDirectories(storage.working());try(var dirs=Files.list(storage.working())){for(Path dir:dirs.toList()){
            String name=dir.getFileName().toString();if(!name.matches("task-[0-9]+")||Files.isSymbolicLink(dir))continue;
            long task=Long.parseLong(name.substring(5));
            if(db.queryForObject("select count(*) from processing_task t join music_version v on v.id=t.output_version_id where t.id=? and t.status='SUCCEEDED' and v.file_status='AVAILABLE'",Integer.class,task)==0)continue;
            if(Files.exists(storage.reports().resolve(name+".publishing")))continue;
            try(var files=Files.walk(dir)){var paths=files.toList();boolean safe=true;for(Path p:paths)if(Files.isSymbolicLink(p)||!p.toRealPath().startsWith(storage.working().toRealPath()))safe=false;
                if(safe)for(Path p:paths.stream().sorted(Comparator.reverseOrder()).toList())Files.delete(p);
            }
        }}}catch(Exception e){issue(storage.working(),"WORK_CLEANUP_REQUIRES_REVIEW");}
    }
    private LinkedHashMap<String,Object> recoveredReport(long task,long version,String hash){
        var report=new LinkedHashMap<String,Object>();report.put("taskId",task);report.put("versionId",version);report.put("status","SUCCEEDED");report.put("sha256",hash);report.put("recovered",true);
        report.put("format",db.queryForObject("select r.detected_format from processing_task t join music_resource r on r.id=t.resource_id where t.id=?",String.class,task));
        report.put("capabilities","UNKNOWN_RECOVERED");report.put("executedActions","UNKNOWN_RECOVERED");report.put("verification","HASH_ONLY_RECOVERED");report.put("playerAcceptance","PENDING USER EXECUTION");return report;
    }
    private void issue(Path path,String code){try{String key=hash(path.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));if(db.queryForObject("select count(*) from recovery_issue where issue_key=?",Integer.class,key)==0)db.update("insert into recovery_issue values(?,?,?)",key,code,now());}catch(Exception ignored){}}
}

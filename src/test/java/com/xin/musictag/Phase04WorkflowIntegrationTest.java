package com.xin.musictag;

import com.xin.musictag.application.*;
import com.xin.musictag.domain.*;
import com.xin.musictag.tagging.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"music.storage.root=.test-data/phase04-workflow-${random.uuid}","spring.datasource.url=jdbc:h2:mem:phase04workflow;MODE=MySQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class Phase04WorkflowIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired AudioFileService uploads;
    @Autowired SingleSongService songs; @Autowired BatchImportService batches; @Autowired AudioTagHandlerRegistry handlers;
    @Autowired JdbcTemplate db; @Autowired StorageSettings storage; @Autowired StorageOperations operations;
    @TempDir Path samples;
    final TagEditPlan keep=new TagEditPlan(FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),UpdateAction.KEEP);
    MockMultipartFile file(String name,byte[] bytes){return new MockMultipartFile("audio",name,"application/octet-stream",bytes);}
    @Test void newFormatsHaveIndependentVersionsThreeStatesAndSafeReports()throws Exception {
        Path cover=Phase04Fixtures.cover(samples);
        for(String format:List.of("m4a","ogg")) {
            Path input=Phase04Fixtures.generate(samples,format);byte[] original=Files.readAllBytes(input),pcm=Phase04Fixtures.pcm(input);
            ResourceRecord resource=uploads.upload(file("sample."+format,original),file("sample.lrc","[00:00.00] 原创歌词".getBytes(java.nio.charset.StandardCharsets.UTF_8)),file("cover.png",Files.readAllBytes(cover)));
            assertEquals("原创标题",songs.metadata(resource.id()).title());assertTrue(songs.preview(resource.id(),keep).capabilities().output());
            TagEditPlan set=new TagEditPlan(FieldChange.set("新标题"),FieldChange.set("新歌手"),FieldChange.set("新专辑"),FieldChange.set("[00:00.00] 新歌词"),UpdateAction.SET);
            ProcessResult first=songs.process(resource.id(),set);assertEquals("SUCCEEDED",first.status());VersionRecord v1=songs.version(first.versionId());
            assertArrayEquals(pcm,Phase04Fixtures.pcm(v1.outputPath()));var metadata=handlers.require(format).read(v1.outputPath());assertEquals("新标题",metadata.title());assertEquals("[00:00.00] 新歌词",metadata.lyrics());assertTrue(metadata.artworkPresent());
            ProcessResult second=songs.process(resource.id(),keep);assertEquals("SUCCEEDED",second.status());VersionRecord v2=songs.version(second.versionId());assertEquals(v1.id(),v2.parentVersionId());assertEquals(metadata,handlers.require(format).read(v2.outputPath()));
            ProcessResult third=songs.process(resource.id(),new TagEditPlan(FieldChange.remove(),FieldChange.remove(),FieldChange.remove(),FieldChange.remove(),UpdateAction.REMOVE));assertEquals("SUCCEEDED",third.status());VersionRecord v3=songs.version(third.versionId());assertEquals(v2.id(),v3.parentVersionId());var removed=handlers.require(format).read(v3.outputPath());assertNull(removed.title());assertNull(removed.lyrics());assertFalse(removed.artworkPresent());assertArrayEquals(pcm,Phase04Fixtures.pcm(v3.outputPath()));
            assertArrayEquals(original,Files.readAllBytes(resource.storagePath()));assertNotEquals(resource.storagePath(),v1.outputPath());
            byte[] download=mvc.perform(get(first.downloadUrl())).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();assertEquals(v1.sha256(),StorageOperations.hash(download));
            String report=new String(songs.report(first.taskId()),java.nio.charset.StandardCharsets.UTF_8);var json=mapper.readTree(report);assertEquals(format,json.get("format").asText());assertTrue(json.get("capabilities").get("lyrics").asBoolean());assertEquals(v1.sha256(),json.get("sha256").asText());assertEquals("WRITE_REREAD_PASSED",json.get("verification").asText());assertNotNull(json.get("executedActions"));assertFalse(report.contains(storage.root().toString()));
            assertTrue(operations.status(first.taskId()).toString().contains("DISABLED"));
        }
    }
    @Test void forgedContainersAndUnsupportedOpusAreRejectedBeforeVersionCreation()throws Exception {
        Path m4a=Phase04Fixtures.generate(samples,"m4a"),opus=Phase04Fixtures.generate(samples,"opus");int versions=db.queryForObject("select count(*) from music_version",Integer.class);
        for(var part:List.of(file("fake.ogg",Files.readAllBytes(opus)),file("fake.mp3",Files.readAllBytes(m4a)),file("broken.m4a","not audio".getBytes()),file("empty.m4a",new byte[0])))mvc.perform(multipart("/api/songs").file(part)).andExpect(status().isUnprocessableEntity());
        var rejected=mvc.perform(multipart("/api/songs").file(file("sample.opus",Files.readAllBytes(opus)))).andExpect(status().isUnprocessableEntity()).andReturn().getResponse().getContentAsString();assertTrue(rejected.contains("UNSUPPORTED_FORMAT"));assertTrue(rejected.contains("UNSUPPORTED"));
        assertEquals(versions,db.queryForObject("select count(*) from music_version",Integer.class));
        Path alac=samples.resolve("alac.m4a");Phase04Fixtures.ffmpeg("-f","lavfi","-i","sine=duration=1","-c:a","alac",alac.toString());assertEquals("UNSUPPORTED_FORMAT",assertThrows(ProcessingException.class,()->uploads.upload(file("alac.m4a",Files.readAllBytes(alac)),null,null)).code());
    }
    @Test void mixedBatchPartialSuccessRetryZipAndUnsupportedAttachments()throws Exception {
        var parts=new ArrayList<org.springframework.web.multipart.MultipartFile>();var paths=new ArrayList<String>();
        for(String format:List.of("mp3","flac","wav","m4a","ogg","opus")){parts.add(file(format+"."+format,Files.readAllBytes(Phase04Fixtures.generate(samples,format))));paths.add(format+"."+format);}
        parts.add(file("broken.ogg","broken".getBytes()));paths.add("broken.ogg");parts.add(file("fake.ogg",Files.readAllBytes(samples.resolve("sample.opus"))));paths.add("fake.ogg");
        for(String name:List.of("m4a","ogg")){parts.add(file(name+".lrc","[00:00.00] 原创绑定歌词".getBytes(java.nio.charset.StandardCharsets.UTF_8)));paths.add(name+".lrc");parts.add(file(name+".png",Files.readAllBytes(Phase04Fixtures.cover(samples))));paths.add(name+".png");}
        BatchTask batch=batches.importFiles(parts,paths);var items=(List<BatchItem>)batches.detail(batch.id()).get("items");BatchItem target=items.stream().filter(i->i.relativePath().equals("m4a.m4a")).findFirst().orElseThrow();
        ResourceRecord resource=songs.requireResource(target.resourceId());Path held=resource.storagePath().resolveSibling("held");Files.move(resource.storagePath(),held);
        try{batches.confirm(batch.id(),Set.of());batches.execute(batch.id(),false);await(batch.id());assertEquals("COMPLETED_WITH_FAILURES",((Map<?,?>)batches.detail(batch.id()).get("task")).get("status"));}
        finally{Files.move(held,resource.storagePath());}
        var before=(List<BatchItem>)batches.detail(batch.id()).get("items");var existing=new HashMap<Long,Long>();before.stream().filter(i->i.outputVersionId()!=null).forEach(i->existing.put(i.id(),i.outputVersionId()));
        batches.execute(batch.id(),true);await(batch.id());var detail=batches.detail(batch.id());items=(List<BatchItem>)detail.get("items");assertEquals(5,items.stream().filter(i->i.status().equals("SUCCESS")).count());assertEquals(1,items.stream().filter(i->i.kind().equals("UNSUPPORTED")).count());for(BatchItem item:items)if(existing.containsKey(item.id()))assertEquals(existing.get(item.id()),item.outputVersionId());
        var zip=new ByteArrayOutputStream();batches.writeZip(batch.id(),zip);var names=new HashSet<String>();try(var in=new ZipInputStream(new ByteArrayInputStream(zip.toByteArray()))){for(ZipEntry entry;(entry=in.getNextEntry())!=null;){names.add(entry.getName());if(entry.getName().equals("report.json")){var report=mapper.readTree(in.readAllBytes());assertEquals(5,report.get("outputs").size());assertNotNull(report.get("itemCapabilities"));}}}assertEquals(6,names.size());assertTrue(names.stream().anyMatch(n->n.endsWith(".m4a")));assertTrue(names.stream().anyMatch(n->n.endsWith(".ogg")));
        for(BatchItem item:items)if(item.outputVersionId()!=null&&List.of("m4a.m4a","ogg.ogg").contains(item.relativePath())){var version=songs.version(item.outputVersionId());var metadata=handlers.require(songs.requireResource(item.resourceId()).format()).read(version.outputPath());assertEquals("[00:00.00] 原创绑定歌词",metadata.lyrics());assertTrue(metadata.artworkPresent());}
    }
    @Test void newFormatsUseExistingBackupFailureHoldAndLifecycle()throws Exception {
        Map<String,String> remoteObjects=new HashMap<>();boolean[] down={true};
        BackupStore remote=new BackupStore(){public boolean ready(){return !down[0];}public void put(String key,byte[] bytes,String hash)throws Exception{if(down[0])throw new IOException("fake remote outage");remoteObjects.put(key,hash);}};
        var settings=new OperationsSettings(true,"https://invalid.example","FAKE","FAKE","fake-bucket","phase04",Duration.ofDays(30),0,0);
        var backup=new StorageOperations(db,storage,settings,remote,Clock.fixed(Instant.now().plus(Duration.ofDays(31)),ZoneOffset.UTC),mapper);
        for(String format:List.of("m4a","ogg")) {
            ResourceRecord resource=uploads.upload(file("sample."+format,Files.readAllBytes(Phase04Fixtures.generate(samples,format))),null,null);var result=songs.process(resource.id(),keep);assertEquals("SUCCEEDED",result.status());VersionRecord version=songs.version(result.versionId());
            backup.retry(version.id());assertTrue(backup.status(result.taskId()).toString().contains("FAILED"));backup.cleanup(false);assertTrue(Files.exists(version.outputPath()));assertTrue(backup.downloadable(version.id()));
            down[0]=false;backup.retry(version.id());int keys=remoteObjects.size();backup.retry(version.id());assertEquals(keys,remoteObjects.size());assertTrue(backup.status(result.taskId()).toString().contains("SUCCEEDED"));
            backup.cleanup(true);assertTrue(Files.exists(version.outputPath()));backup.cleanup(false);assertFalse(backup.downloadable(version.id()));mvc.perform(get(result.downloadUrl())).andExpect(status().isUnprocessableEntity());down[0]=true;
        }
        assertTrue(remoteObjects.keySet().stream().allMatch(k->k.startsWith("phase04/")));
    }
    void await(long id)throws Exception {for(int attempt=0;attempt<1000;attempt++){String status=((Map<?,?>)batches.detail(id).get("task")).get("status").toString();if(!status.equals("RUNNING"))return;Thread.sleep(10);}fail("Batch timeout");}
    @Test void interruptedPublicationKeepsActualFormatAndVerificationInReport()throws Exception {
        ResourceRecord resource=uploads.upload(file("sample.m4a",Files.readAllBytes(Phase04Fixtures.generate(samples,"m4a"))),null,null);var result=songs.process(resource.id(),keep);var version=songs.version(result.versionId());
        var report=mapper.readValue(songs.report(result.taskId()),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});Files.delete(Path.of(songs.task(result.taskId()).reportPath()));
        operations.preparePublication(result.taskId(),resource.id(),null,null,version.outputPath(),version.sha256(),Map.of("format","m4a","capabilities",handlers.require("m4a").capabilities(),"executedActions",report.get("executedActions"),"verification","WRITE_REREAD_PASSED"));
        operations.recover();operations.recover();var recovered=mapper.readTree(songs.report(result.taskId()));assertEquals("m4a",recovered.get("format").asText());assertEquals("WRITE_REREAD_PASSED",recovered.get("verification").asText());assertTrue(recovered.get("capabilities").get("output").asBoolean());assertNotNull(recovered.get("executedActions"));
    }
}

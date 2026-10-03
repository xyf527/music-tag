package com.xin.musictag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.application.BatchImportService;
import com.xin.musictag.application.SingleSongService;
import com.xin.musictag.domain.BatchItem;
import com.xin.musictag.domain.BatchRepository;
import com.xin.musictag.tagging.AudioTagHandlerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"music.storage.root=.test-data/phase02-test-${random.uuid}"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BatchImportIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired SingleSongService songs;
    @Autowired BatchImportService batches;
    @Autowired BatchRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired AudioTagHandlerRegistry handlers;
    @TempDir static Path samples;

    @BeforeAll static void samples() throws Exception {
        for (String format : List.of("mp3","flac","wav")) {
            run(List.of("-f","lavfi","-i","sine=frequency=440:duration=1","-ar","44100","-ac","1",
                    "-metadata","title=Original Song","-metadata","artist=Original Artist",samples.resolve("song." + format).toString()));
        }
        for (String name : List.of("cover.png","other.png")) {
            BufferedImage image = new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) image.setRGB(x,y,(name.equals("cover.png") ? Color.RED : Color.BLUE).getRGB());
            ImageIO.write(image,"png",samples.resolve(name).toFile());
        }
    }

    @Test void uploadsUniqueMp3FlacBindingsAndStreamsConsistentZip() throws Exception {
        long id = upload(part("a/song.mp3"), part("b/song.flac"),
                text("a/song.lrc","[00:00.00]MP3 lyric"), image("a/song.png"),
                text("b/song.lrc","[00:00.00]FLAC lyric"), image("b/song.png"), text("note.txt","unsupported"));
        assertEquals(2, read(id).path("counts").path("PENDING").asInt());
        assertEquals(1, read(id).path("counts").path("UNSUPPORTED").asInt());
        confirm(id); execute(id); JsonNode completed = waitFor(id);
        assertEquals(2,completed.path("counts").path("SUCCESS").asInt());
        for (BatchItem item : repository.items(id)) if (item.status().equals("SUCCESS")) {
            var metadata = handlers.require(item.relativePath().endsWith("mp3") ? "mp3" : "flac").read(songs.version(item.outputVersionId()).outputPath());
            assertTrue(metadata.lyrics().contains(item.relativePath().endsWith("mp3") ? "MP3 lyric" : "FLAC lyric"));
            assertTrue(metadata.artworkPresent());
        }
        byte[] zip = zip(id);
        Map<String,byte[]> contents = unzip(zip);
        assertEquals(3, contents.size()); assertTrue(contents.containsKey("report.json"));
        JsonNode report = mapper.readTree(contents.get("report.json"));
        assertEquals(read(id),report);
        long successes = report.path("items").findValuesAsText("status").stream().filter("SUCCESS"::equals).count();
        assertEquals(successes,contents.keySet().stream().filter(name -> name.startsWith("outputs/")).count());
        contents.keySet().forEach(name -> { assertFalse(name.startsWith("/")); assertFalse(name.contains("..")); });
        assertFalse(report.toString().contains("storagePath")); assertFalse(report.toString().contains("outputPath"));
        String artifacts = System.getProperty("phase02.artifacts.dir");
        if (artifacts != null) {
            Path root = Path.of(artifacts); Files.createDirectories(root);
            Files.write(root.resolve("batch.zip"),zip); Files.write(root.resolve("report.json"),contents.get("report.json"));
            for (BatchItem item : repository.items(id)) if (item.status().equals("SUCCESS"))
                Files.copy(songs.version(item.outputVersionId()).outputPath(),root.resolve("success-" + item.id() + (item.relativePath().endsWith("mp3") ? ".mp3" : ".flac")), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test void duplicateAudioStemNeverSharesAttachments() throws Exception {
        long id = upload(part("a/song.mp3"),part("a/song.flac"),text("a/song.lrc","[00]x"),image("a/song.png"));
        assertEquals(2,read(id).path("counts").path("CONFLICT").asInt());
        for (BatchItem item : audios(id)) { assertNull(item.lyricsItemId()); assertNull(item.coverItemId()); }
        mvc.perform(post("/api/batches/{id}/confirm",id)).andExpect(status().isBadRequest());
        BatchItem first = audios(id).get(0), second = audios(id).get(1);
        long lyric = asset(id,"LYRICS",0).id(), cover = asset(id,"COVER",0).id();
        bind(id,first.id(),lyric,cover);
        mvc.perform(post("/api/batches/{id}/items/{item}/bindings",id,second.id())
                .contentType(MediaType.APPLICATION_JSON).content(binding(lyric,cover))).andExpect(status().isBadRequest());
        bind(id,second.id(),null,null); confirm(id);
    }

    @Test void strongLrcTagsMatchOnlyUniqueTitleAndArtist() throws Exception {
        long id = upload(part("a/source.mp3"),text("other-name.lrc","[ti: ORIGINAL SONG ][ar:Original Artist]\n[00]matched"));
        BatchItem audio = audios(id).get(0);
        assertNotNull(audio.lyricsItemId()); assertEquals("LRC_TITLE_ARTIST",audio.matchBasis());
        long ambiguous = upload(part("source.mp3"),text("one.lrc","[ti:Original Song][ar:Original Artist][00]one"),text("two.lrc","[ti:Original Song][ar:Original Artist][00]two"));
        assertEquals("CONFLICT",audios(ambiguous).get(0).status());
        long duplicateAudio = upload(part("one.mp3"),part("two.flac"),text("tags.lrc","[ti:Original Song][ar:Original Artist][00]x"));
        assertEquals(2,read(duplicateAudio).path("counts").path("CONFLICT").asInt());
        long partialTags = upload(part("audio.mp3"),text("lyrics.lrc","[ti:Original Song][00]missing artist"));
        assertNull(audios(partialTags).get(0).lyricsItemId());
    }

    @Test void directoryAndTagConflictsRequireManualDecision() throws Exception {
        long id = upload(part("a/song.mp3"),text("b/song.lrc","[00]other directory"));
        assertEquals("CONFLICT",audios(id).get(0).status());
        long tags = upload(part("song.mp3"),text("song.lrc","[ti:Different][ar:Other][00]conflict"));
        assertEquals("CONFLICT",audios(tags).get(0).status());
        long multiple = upload(part("song.mp3"),text("song.lrc","[00]one"),text("song.LRC","[00]two"));
        assertEquals("CONFLICT",audios(multiple).get(0).status());
    }

    @Test void manualBindRebindUnbindAndExecutionUseImmutableSnapshot() throws Exception {
        long id = upload(part("track.mp3"),text("first.lrc","[00]first"),text("second.lrc","[00]second"),image("first.png"),image("second.png"));
        BatchItem audio = audios(id).get(0); long first = asset(id,"LYRICS",0).id(), second = asset(id,"LYRICS",1).id();
        long cover = asset(id,"COVER",0).id(), otherCover = asset(id,"COVER",1).id();
        bind(id,audio.id(),first,cover); assertEquals(first,audios(id).get(0).lyricsItemId());
        bind(id,audio.id(),second,otherCover); assertEquals(second,audios(id).get(0).lyricsItemId());
        assertEquals("UNBOUND",asset(id,"LYRICS",0).status());
        bind(id,audio.id(),null,null); assertNull(audios(id).get(0).lyricsItemId()); assertNull(audios(id).get(0).coverItemId());
        bind(id,audio.id(),first,cover); confirm(id);
        String snapshot = repository.require(id).planJson();
        mvc.perform(post("/api/batches/{id}/items/{item}/bindings",id,audio.id()).contentType(MediaType.APPLICATION_JSON).content(binding(second,otherCover))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/batches/{id}/confirm",id)).andExpect(status().isBadRequest());
        // Simulate a later mutable-row change outside the application. Execution must still use the snapshot.
        jdbc.update("update batch_item set lyrics_item_id=?,cover_item_id=? where id=?",second,otherCover,audio.id());
        jdbc.update("update batch_attachment set lyrics_text='[00]changed' where item_id=?",first);
        assertEquals(snapshot,repository.require(id).planJson());
        execute(id); waitFor(id); BatchItem done = audios(id).get(0);
        var metadata = handlers.require("mp3").read(songs.version(done.outputVersionId()).outputPath());
        assertEquals("[00]first",metadata.lyrics()); assertTrue(metadata.artworkPresent());
        byte[] expectedCover = Files.readAllBytes(samples.resolve("cover.png"));
        // Snapshot preserves the first cover hash, independently of the mutable binding row.
        assertTrue(snapshot.contains(repository.attachment(cover).sha256()));
        byte[] actualCover = org.jaudiotagger.audio.AudioFileIO.read(songs.version(done.outputVersionId()).outputPath().toFile()).getTag().getFirstArtwork().getBinaryData();
        assertArrayEquals(expectedCover,actualCover);
    }

    @Test void partialFailureRetryNeverDuplicatesSuccessfulVersions() throws Exception {
        long id = upload(part("good.mp3"),part("other.flac"),part("missing.mp3"));
        BatchItem failed = audios(id).get(2);
        Path original = songs.requireResource(failed.resourceId()).storagePath(); byte[] bytes = Files.readAllBytes(original); Files.delete(original);
        confirm(id); execute(id); JsonNode result = waitFor(id);
        assertEquals(2,result.path("counts").path("SUCCESS").asInt()); assertEquals(1,result.path("counts").path("FAILED").asInt());
        List<Long> versionIds = audios(id).stream().filter(i -> i.status().equals("SUCCESS")).map(BatchItem::outputVersionId).toList();
        JsonNode failedReport = mapper.readTree(unzip(zip(id)).get("report.json"));
        assertEquals("FAILED",failedReport.path("items").get(2).path("status").asText());
        String artifacts = System.getProperty("phase02.artifacts.dir");
        if (artifacts != null) { Path root = Path.of(artifacts); Files.createDirectories(root); Files.write(root.resolve("partial.zip"),zip(id)); mapper.writerWithDefaultPrettyPrinter().writeValue(root.resolve("partial-report.json").toFile(),failedReport); }
        Files.write(original,bytes); mvc.perform(post("/api/batches/{id}/retry",id)).andExpect(status().isOk()); waitFor(id);
        assertEquals(3,read(id).path("counts").path("SUCCESS").asInt());
        assertEquals(versionIds,audios(id).subList(0,2).stream().map(BatchItem::outputVersionId).toList());
        int count = jdbc.queryForObject("select count(*) from music_version where batch_item_id in (select id from batch_item where batch_task_id=?)",Integer.class,id);
        mvc.perform(post("/api/batches/{id}/retry",id)).andExpect(status().isOk()); waitFor(id);
        assertEquals(count,jdbc.queryForObject("select count(*) from music_version where batch_item_id in (select id from batch_item where batch_task_id=?)",Integer.class,id));
    }

    @Test void restartRecoversPublishedAndPendingItemsAndAllowsInterruptedRetry() throws Exception {
        long id = upload(part("one.mp3"),part("two.flac"),part("three.mp3")); confirm(id); execute(id); waitFor(id);
        BatchItem published = audios(id).get(0); long version = published.outputVersionId();
        // Simulate a crash after publication but before the batch row received the result.
        jdbc.update("update batch_item set status='RUNNING',output_version_id=null where id=?",published.id());
        repository.taskStatus(id,"RUNNING"); batches.recoverInterruptedRuns();
        assertEquals("SUCCESS",audios(id).get(0).status()); assertEquals(version,audios(id).get(0).outputVersionId());
        long unfinished = upload(part("pending.mp3"),part("running.flac")); confirm(unfinished);
        repository.taskStatus(unfinished,"RUNNING");
        repository.itemStatus(audios(unfinished).get(1).id(),"RUNNING","WRITE",null,null,null);
        batches.recoverInterruptedRuns();
        assertEquals(2,read(unfinished).path("counts").path("INTERRUPTED").asInt());
        assertEquals("INTERRUPTED",repository.require(unfinished).status());
        mvc.perform(post("/api/batches/{id}/retry",unfinished)).andExpect(status().isOk()); waitFor(unfinished);
        assertEquals(2,read(unfinished).path("counts").path("SUCCESS").asInt());
        mvc.perform(post("/api/batches/{id}/retry",id)).andExpect(status().isOk()); waitFor(id);
        assertEquals(version,audios(id).get(0).outputVersionId());
    }

    @Test void damagedAudioDoesNotDiscardBatchAndForeignBindingsAreRejected() throws Exception {
        long id = upload(part("good.mp3"),text("broken.mp3","not audio"));
        assertEquals(1,read(id).path("counts").path("FAILED").asInt()); confirm(id); execute(id); waitFor(id);
        assertEquals(1,read(id).path("counts").path("SUCCESS").asInt());
        long foreign = upload(text("foreign.lrc","[00]x")), other = upload(part("other.mp3"));
        mvc.perform(post("/api/batches/{id}/items/{item}/bindings",other,audios(other).get(0).id()).contentType(MediaType.APPLICATION_JSON).content(binding(asset(foreign,"LYRICS",0).id(),null))).andExpect(status().isBadRequest());
    }

    @Test void rejectsUnsafePathsEmptyInputsAndDisplaysSafePageEntrypoints() throws Exception {
        for (String path : List.of("../song.mp3","/song.mp3","C:/song.mp3","a/../song.mp3","a/./song.mp3","a//song.mp3","a/..")) {
            mvc.perform(multipart("/api/batches").file(new MockMultipartFile("files","song.mp3","audio/mpeg",Files.readAllBytes(samples.resolve("song.mp3")))).param("paths",path)).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/api/batches").file(new MockMultipartFile("files","empty.lrc","text/plain",new byte[0])).param("paths","empty.lrc")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/batches").file(new MockMultipartFile("files","unsafe..txt","text/plain","text".getBytes())).param("paths","unsafe..txt"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("文件名不安全，请检查后重新选择"));
        String page = mvc.perform(get("/batch")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(page.contains("选择多个文件")); assertTrue(page.contains("选择目录"));
        long id = upload(text("<img onerror=alert(1)>.txt","ignored"));
        assertEquals("<img onerror=alert(1)>.txt",read(id).path("items").get(0).path("relativePath").asText());
        String javascript = Files.readString(Path.of("src/main/resources/static/batch.js"));
        assertFalse(javascript.contains("innerHTML")); assertTrue(javascript.contains("textContent")); assertTrue(javascript.contains("replaceChildren"));
        mvc.perform(get("/api/batches/policy")).andExpect(status().isOk()).andExpect(jsonPath("$.maxFiles").value(500)).andExpect(jsonPath("$.concurrency").value(2));
    }
    @Test void browserDirectoryMultipartNamesAreSafelyPreserved() throws Exception {
        long id = mapper.readTree(mvc.perform(multipart("/api/batches")
                .file(new MockMultipartFile("files","album/song.mp3","audio/mpeg",Files.readAllBytes(samples.resolve("song.mp3"))))
                .file(new MockMultipartFile("files","album/song.lrc","text/plain","[00]directory".getBytes()))
                .param("paths","album/song.mp3","album/song.lrc")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("id").asLong();
        assertEquals("album/song.mp3",audios(id).get(0).relativePath());
        assertNotNull(audios(id).get(0).lyricsItemId());
        confirm(id); execute(id); waitFor(id);
        assertEquals("[00]directory",handlers.require("mp3").read(songs.version(audios(id).get(0).outputVersionId()).outputPath()).lyrics());
    }

    record Input(String path, byte[] bytes) { }
    static Input part(String path) throws Exception { return new Input(path,Files.readAllBytes(samples.resolve("song." + path.substring(path.lastIndexOf('.')+1)))); }
    static Input text(String path,String text) { return new Input(path,text.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static Input image(String path) throws Exception { return new Input(path,Files.readAllBytes(samples.resolve(path.contains("second") ? "other.png" : "cover.png"))); }
    long upload(Input... inputs) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/batches");
        for (Input input : inputs) { request.file(new MockMultipartFile("files",input.path().substring(input.path().lastIndexOf('/')+1),"application/octet-stream",input.bytes())); request.param("paths",input.path()); }
        return mapper.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("id").asLong();
    }
    JsonNode read(long id) throws Exception { return mapper.readTree(mvc.perform(get("/api/batches/{id}",id)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()); }
    List<BatchItem> audios(long id) { return repository.items(id).stream().filter(i -> i.kind().equals("AUDIO")).toList(); }
    BatchItem asset(long id,String kind,int index) { return repository.items(id).stream().filter(i -> i.kind().equals(kind)).toList().get(index); }
    void bind(long id,long item,Long lyrics,Long cover) throws Exception { mvc.perform(post("/api/batches/{id}/items/{item}/bindings",id,item).contentType(MediaType.APPLICATION_JSON).content(binding(lyrics,cover))).andExpect(status().isOk()); }
    String binding(Long lyrics,Long cover) { return "{\"lyricsItemId\":" + lyrics + ",\"coverItemId\":" + cover + "}"; }
    void confirm(long id) throws Exception { mvc.perform(post("/api/batches/{id}/confirm",id)).andExpect(status().isOk()); }
    void execute(long id) throws Exception { mvc.perform(post("/api/batches/{id}/execute",id)).andExpect(status().isOk()); }
    JsonNode waitFor(long id) throws Exception { for (int n=0;n<100;n++) { JsonNode detail=read(id); if (detail.path("task").path("status").asText().startsWith("COMPLETED")) return detail; Thread.sleep(50); } fail("batch did not complete"); return null; }
    byte[] zip(long id) throws Exception {
        var result = mvc.perform(get("/api/batches/{id}/zip",id)).andExpect(request().asyncStarted()).andReturn();
        return mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }
    static Map<String,byte[]> unzip(byte[] bytes) throws Exception {
        Map<String,byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) { for (var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) { assertNull(entries.put(entry.getName(),zip.readAllBytes()),"duplicate ZIP name"); } }
        return entries;
    }
    static void run(List<String> args) throws Exception {
        var command = new ArrayList<String>(List.of("ffmpeg","-hide_banner","-loglevel","error","-y")); command.addAll(args);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start(); String output = new String(process.getInputStream().readAllBytes()); assertEquals(0,process.waitFor(),output);
    }
}

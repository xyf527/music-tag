package com.xin.musictag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.application.SingleSongService;
import com.xin.musictag.tagging.AudioTagHandlerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"music.storage.root=.test-data/phase02-test-${random.uuid}","MUSIC_BATCH_CONCURRENCY=1"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class BatchImportIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired SingleSongService songs; @Autowired AudioTagHandlerRegistry handlers;
 @TempDir static Path samples;
 @BeforeAll static void samples() throws Exception { run(List.of("-f","lavfi","-i","sine=frequency=440:duration=1","-ar","44100","-ac","1",samples.resolve("x.wav").toString())); run(List.of("-i",samples.resolve("x.wav").toString(),samples.resolve("song.mp3").toString())); run(List.of("-i",samples.resolve("x.wav").toString(),samples.resolve("song.flac").toString())); BufferedImage image=new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB);image.setRGB(0,0, Color.RED.getRGB());ImageIO.write(image,"png",samples.resolve("song.png").toFile()); }
 @Test void uploadsMatchesConfirmsProcessesAndExportsZip() throws Exception {
  byte[] mp3=Files.readAllBytes(samples.resolve("song.mp3")),flac=Files.readAllBytes(samples.resolve("song.flac")),cover=Files.readAllBytes(samples.resolve("song.png"));
  var request=multipart("/api/batches").file(new MockMultipartFile("files","song.mp3","audio/mpeg",mp3)).file(new MockMultipartFile("files","other.flac","audio/flac",flac)).file(new MockMultipartFile("files","song.lrc","text/plain","[00:00.00]批量歌词".getBytes())).file(new MockMultipartFile("files","song.png","image/png",cover)).file(new MockMultipartFile("files","bad.txt","text/plain","x".getBytes())).param("paths","album/song.mp3","album/other.flac","album/song.lrc","album/song.png","album/bad.txt");
  long id=mapper.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).get("id").asLong();
  JsonNode before=read(id); assertEquals(2,before.get("counts").get("PENDING").asInt()); assertEquals(1,before.get("counts").get("UNSUPPORTED").asInt());
  mvc.perform(post("/api/batches/{id}/confirm",id)).andExpect(status().isOk()); mvc.perform(post("/api/batches/{id}/execute",id)).andExpect(status().isOk());
  JsonNode completed=waitFor(id); assertEquals(2,completed.get("counts").get("SUCCESS").asInt());
  for(JsonNode item:completed.get("items")) if("SUCCESS".equals(item.get("status").asText()) && item.get("relativePath").asText().endsWith("song.mp3")) { var metadata=handlers.require("mp3").read(songs.version(item.get("outputVersionId").asLong()).outputPath()); assertEquals("[00:00.00]批量歌词",metadata.lyrics()); assertTrue(metadata.artworkPresent()); }
  byte[] zip=mvc.perform(get("/api/batches/{id}/zip",id)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray(); boolean report=false;int outputs=0;try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(zip))){for(var e=in.getNextEntry();e!=null;e=in.getNextEntry()){if(e.getName().equals("report.json"))report=true;if(e.getName().startsWith("outputs/"))outputs++;}}assertTrue(report);assertEquals(2,outputs);
 }
 @Test void rejectsAmbiguousMatchWithoutGuessing() throws Exception { byte[] mp3=Files.readAllBytes(samples.resolve("song.mp3")); long id=mapper.readTree(mvc.perform(multipart("/api/batches").file(new MockMultipartFile("files","song.mp3","audio/mpeg",mp3)).file(new MockMultipartFile("files","song.lrc","text/plain","[00]a".getBytes())).file(new MockMultipartFile("files","song.LRC","text/plain","[00]b".getBytes())).param("paths","a/song.mp3","a/song.lrc","a/song.LRC")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).get("id").asLong(); assertTrue(read(id).get("items").toString().contains("CONFLICT")); mvc.perform(post("/api/batches/{id}/confirm",id)).andExpect(status().isBadRequest()); }
 @Test void rejectsSharedAttachmentForSameStemDifferentFormats() throws Exception {byte[] mp3=Files.readAllBytes(samples.resolve("song.mp3")),flac=Files.readAllBytes(samples.resolve("song.flac"));long id=mapper.readTree(mvc.perform(multipart("/api/batches").file(new MockMultipartFile("files","song.mp3","audio/mpeg",mp3)).file(new MockMultipartFile("files","song.flac","audio/flac",flac)).file(new MockMultipartFile("files","song.lrc","text/plain","[00]x".getBytes())).param("paths","a/song.mp3","a/song.flac","a/song.lrc")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).get("id").asLong();assertEquals(2,read(id).get("counts").get("CONFLICT").asInt());}
 private JsonNode read(long id)throws Exception{return mapper.readTree(mvc.perform(get("/api/batches/{id}",id)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());}
 private JsonNode waitFor(long id)throws Exception{for(int i=0;i<80;i++){JsonNode n=read(id);if(n.get("task").get("status").asText().startsWith("COMPLETED"))return n;Thread.sleep(100);}fail("batch did not complete");return null;}
 private static void run(List<String>a)throws Exception{var c=new java.util.ArrayList<String>();c.addAll(List.of("ffmpeg","-hide_banner","-loglevel","error","-y"));c.addAll(a);Process p=new ProcessBuilder(c).start();assertEquals(0,p.waitFor(),new String(p.getErrorStream().readAllBytes()));}
}

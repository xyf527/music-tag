package com.xin.musictag.web;

import com.xin.musictag.application.AudioFileService;
import com.xin.musictag.application.SingleSongService;
import com.xin.musictag.domain.*;
import com.xin.musictag.tagging.AudioMetadata;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
public class SongController {
    private final AudioFileService files; private final SingleSongService songs;
    public SongController(AudioFileService files, SingleSongService songs) { this.files = files; this.songs = songs; }
    @GetMapping("/") public String index() { return "index"; }
    @GetMapping("/api/upload-policy") @ResponseBody public Map<String,Object> uploadPolicy() {
        return Map.of("formats", java.util.List.of("MP3", "FLAC", "WAV"), "maxAudioBytes", files.maxAudioBytes(), "maxAudioMegabytes", files.maxAudioMegabytes(), "maxRequestBytes", files.maxRequestBytes());
    }
    @PostMapping(value = "/api/songs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody public Map<String,Object> upload(@RequestPart("audio") MultipartFile audio,
                                                   @RequestPart(value = "lyrics", required = false) MultipartFile lyrics,
                                                   @RequestPart(value = "cover", required = false) MultipartFile cover) {
        ResourceRecord r = files.upload(audio, lyrics, cover); AudioMetadata m = songs.metadata(r.id());
        Map<String,Object> result = new java.util.LinkedHashMap<>(); result.put("resourceId", r.id()); result.put("originalFilename", r.originalFilename()); result.put("format", r.format()); result.put("size", r.byteSize()); result.put("sha256", r.sha256()); result.put("metadata", m); return result;
    }
    @GetMapping("/api/songs/{id}") @ResponseBody public Map<String,Object> metadata(@PathVariable long id) { ResourceRecord r = songs.requireResource(id); return Map.of("resource", r.originalFilename(), "format", r.format(), "size", r.byteSize(), "metadata", songs.metadata(id)); }
    @PostMapping("/api/songs/{id}/preview") @ResponseBody public PreviewResult preview(@PathVariable long id, @RequestBody EditRequest request) { return songs.preview(id, request.toPlan()); }
    @PostMapping("/api/songs/{id}/process") @ResponseBody public ProcessResult process(@PathVariable long id, @RequestBody EditRequest request) { return songs.process(id, request.toPlan()); }
    @GetMapping("/api/tasks/{id}") @ResponseBody public TaskResponse task(@PathVariable long id) { TaskRecord task = songs.task(id); return TaskResponse.from(task); }
    @GetMapping("/api/tasks/{id}/report") public ResponseEntity<ByteArrayResource> report(@PathVariable long id) { return download(songs.report(id), "task-" + id + ".json", MediaType.APPLICATION_JSON); }
    @GetMapping("/api/versions/{id}/download") public ResponseEntity<ByteArrayResource> version(@PathVariable long id) { VersionRecord v = songs.version(id); try { String filename = v.outputPath().getFileName().toString(); if (!filename.matches("version-[a-f0-9-]+\\.(mp3|flac)")) throw new ProcessingException("FILE_IO_ERROR", "DOWNLOAD", "Output filename is invalid"); return download(songs.outputBytes(v), filename, MediaType.APPLICATION_OCTET_STREAM); } catch (ProcessingException e) { throw e; } catch (Exception e) { throw new ProcessingException("FILE_IO_ERROR", "DOWNLOAD", "Output is unavailable"); } }
    private static ResponseEntity<ByteArrayResource> download(byte[] bytes, String name, MediaType type) { return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString()).body(new ByteArrayResource(bytes)); }
}

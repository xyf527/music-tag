package com.xin.musictag.web;

import com.xin.musictag.application.SingleTrackService;
import com.xin.musictag.domain.AudioResource;
import com.xin.musictag.domain.EditPlan;
import com.xin.musictag.domain.ProcessingResult;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/single")
public final class SingleTrackController {
    private final SingleTrackService service;
    public SingleTrackController(SingleTrackService service) { this.service = service; }
    @PostMapping("/upload") public AudioResource upload(@RequestParam MultipartFile audio) throws Exception { return service.upload(audio); }
    @PostMapping("/execute") public ProcessingResult execute(@RequestBody EditRequest request) throws Exception {
        return service.execute(request.resource(), request.plan());
    }
    @GetMapping("/download") public ResponseEntity<FileSystemResource> download(@RequestParam String versionId) throws Exception {
        return ResponseEntity.ok(new FileSystemResource(service.download(versionId)));
    }
    public record EditRequest(AudioResource resource, EditPlan plan) {}
}

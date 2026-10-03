package com.xin.musictag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.audio.AudioTagHandler;
import com.xin.musictag.domain.*;
import com.xin.musictag.storage.FileStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public final class SingleTrackService {
    private final FileStore files;
    private final TaskRepository tasks;
    private final ObjectMapper json = new ObjectMapper();
    public SingleTrackService(FileStore files, TaskRepository tasks) { this.files = files; this.tasks = tasks; }

    public AudioResource upload(MultipartFile upload) throws Exception {
        if (upload == null || upload.isEmpty()) throw new IOException("empty audio upload");
        String name = upload.getOriginalFilename();
        Path safe = files.safeFilename(name);
        String format = extension(safe.getFileName().toString());
        if (!Map.of("mp3", true, "flac", true, "wav", true).containsKey(format)) throw new IOException("unsupported audio format");
        Path dir = files.newDirectory("uploads");
        Path target = dir.resolve(safe.getFileName());
        upload.transferTo(target);
        AudioTagHandler handler = handler(format);
        return handler.read(target, name);
    }

    public ProcessingResult execute(AudioResource resource, EditPlan plan) throws Exception {
        String taskId = UUID.randomUUID().toString();
        Path workDir = files.newDirectory("working");
        Path copy = workDir.resolve(resource.originalFilename());
        Files.copy(resource.path(), copy);
        AudioTagHandler handler = handler(resource.format());
        if (!handler.supports(plan)) {
            ProcessingResult failed = new ProcessingResult(taskId, "FAILED", "PREPARED", "UNSUPPORTED_FORMAT", null, null,
                    resource.sha256(), null, Instant.now());
            tasks.save(new TaskRecord(taskId, failed.status(), failed.stage(), failed.errorCode(), resource.id(), null, null, failed.completedAt()));
            return failed;
        }
        handler.write(copy, plan, null);
        AudioResource verified = handler.read(copy, resource.originalFilename());
        Path outputDir = files.newDirectory("outputs");
        Path output = outputDir.resolve(resource.originalFilename());
        Files.copy(copy, output);
        String versionId = UUID.randomUUID().toString();
        Path reportDir = files.newDirectory("reports");
        Path report = reportDir.resolve(taskId + ".json");
        ProcessingResult result = new ProcessingResult(taskId, "SUCCEEDED", "PUBLISHED", null, versionId, output,
                resource.sha256(), report.toString(), Instant.now());
        json.writeValue(report.toFile(), Map.of("taskId", taskId, "status", result.status(), "stage", result.stage(),
                "versionId", versionId, "resourceId", resource.id(), "originalSha256", resource.sha256(),
                "outputFilename", verified.originalFilename()));
        tasks.save(new TaskRecord(taskId, result.status(), result.stage(), null, resource.id(), versionId, report.toString(), result.completedAt()));
        return result;
    }

    private AudioTagHandler handler(String format) { return new com.xin.musictag.audio.JaudiotaggerHandler(format); }
    private static String extension(String name) { int dot = name.lastIndexOf('.'); return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(); }
}

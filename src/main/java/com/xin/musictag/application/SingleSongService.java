package com.xin.musictag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.domain.*;
import com.xin.musictag.tagging.*;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;

@Service
public class SingleSongService {
    private final ResourceRepository resources; private final TaskRepository tasks; private final VersionRepository versions;
    private final AudioTagHandlerRegistry handlers; private final StorageSettings storage; private final ObjectMapper mapper;
    public SingleSongService(ResourceRepository resources, TaskRepository tasks, VersionRepository versions,
                             AudioTagHandlerRegistry handlers, StorageSettings storage, ObjectMapper mapper) {
        this.resources = resources; this.tasks = tasks; this.versions = versions; this.handlers = handlers; this.storage = storage; this.mapper = mapper;
    }
    public ResourceRecord requireResource(long id) { return resources.find(id).orElseThrow(() -> new ProcessingException("FILE_IO_ERROR", "READ", "Resource not found")); }
    public AudioMetadata metadata(long id) { ResourceRecord r = requireResource(id); try { return handlers.require(r.format()).read(r.storagePath()); } catch (Exception e) { throw new ProcessingException("CORRUPT_AUDIO", "READ", "Audio metadata cannot be read"); } }
    public PreviewResult preview(long id, TagEditPlan plan) {
        ResourceRecord resource = requireResource(id); AudioTagHandler handler = handlers.require(resource.format()); AudioMetadata current = metadata(id);
        validateCapabilities(handler, plan);
        List<String> changes = new ArrayList<>(); List<String> warnings = new ArrayList<>();
        describe(changes, "title", current.title(), plan.title()); describe(changes, "artist", current.artist(), plan.artist()); describe(changes, "album", current.album(), plan.album());
        if (plan.lyrics().action() != UpdateAction.KEEP) changes.add("lyrics: " + plan.lyrics().action());
        if (plan.artwork() != UpdateAction.KEEP) changes.add("artwork: " + plan.artwork());
        if (!handler.capabilities().lyrics()) warnings.add("WAV lyrics are unsupported by the verified adapter");
        if (!handler.capabilities().artwork()) warnings.add("WAV artwork is unsupported by the verified adapter");
        return new PreviewResult(id, current, plan, changes, warnings, handler.capabilities());
    }
    public ProcessResult process(long id, TagEditPlan plan) {
        ResourceRecord resource = requireResource(id); AudioTagHandler handler = handlers.require(resource.format()); validateCapabilities(handler, plan);
        TaskRecord task = tasks.create(id); Path work = storage.working().resolve("task-" + task.id()); Path output;
        try {
            tasks.updateStatus(task.id(), "RUNNING", "COPYING", null, null); Files.createDirectories(work); Path working = work.resolve("working." + resource.format());
            Files.copy(resource.storagePath(), working, StandardCopyOption.REPLACE_EXISTING); output = storage.outputs().resolve("resource-" + id).resolve("version-" + UUID.randomUUID() + "." + resource.format());
            Files.createDirectories(output.getParent());
            TagWritePlan write = new TagWritePlan(plan.title(), plan.artist(), plan.album(), plan.lyrics(), plan.artwork(), resource.coverPath());
            tasks.updateStatus(task.id(), "RUNNING", "WRITING", null, null); handler.write(working, write);
            tasks.updateStatus(task.id(), "RUNNING", "VALIDATING", null, null); AudioMetadata after = handler.read(working); verify(plan, after);
            Files.move(working, output, StandardCopyOption.ATOMIC_MOVE); String hash = AudioFileService.sha256(output);
            VersionRecord version = versions.create(id, null, task.id(), output, hash); tasks.attachVersion(task.id(), version.id()); tasks.updateStatus(task.id(), "SUCCEEDED", "PUBLISHED", null, null);
            Path report = writeReport(task.id(), resource, plan, after, version, null); tasks.attachReport(task.id(), report);
            deleteTree(work); return new ProcessResult(task.id(), version.id(), "SUCCEEDED", "/api/versions/" + version.id() + "/download", "/api/tasks/" + task.id() + "/report", null, null);
        } catch (ProcessingException e) { tasks.updateStatus(task.id(), "FAILED", e.stage(), e.code(), e.getMessage()); writeReportSafe(task.id(), resource, plan, null, null, e); deleteTree(work); return new ProcessResult(task.id(), 0, "FAILED", null, "/api/tasks/" + task.id() + "/report", e.code(), e.getMessage());
        } catch (Exception e) { tasks.updateStatus(task.id(), "FAILED", "INTERNAL", "INTERNAL_ERROR", "Processing failed"); writeReportSafe(task.id(), resource, plan, null, null, e); deleteTree(work); return new ProcessResult(task.id(), 0, "FAILED", null, "/api/tasks/" + task.id() + "/report", "INTERNAL_ERROR", "Processing failed"); }
    }
    public TaskRecord task(long id) { return tasks.require(id); }
    public VersionRecord version(long id) { return versions.require(id); }
    public byte[] report(long id) { TaskRecord t = tasks.require(id); try { return Files.readAllBytes(Path.of(t.reportPath())); } catch (Exception e) { throw new ProcessingException("FILE_IO_ERROR", "REPORT", "Report is unavailable"); } }
    private void validateCapabilities(AudioTagHandler h, TagEditPlan p) { if (!h.capabilities().lyrics() && p.lyrics().action() != UpdateAction.KEEP) throw new ProcessingException("UNSUPPORTED_FORMAT", "PREVIEW", "Lyrics operation is unsupported for " + h.format().toUpperCase(Locale.ROOT)); if (!h.capabilities().artwork() && p.artwork() != UpdateAction.KEEP) throw new ProcessingException("UNSUPPORTED_FORMAT", "PREVIEW", "Artwork operation is unsupported for " + h.format().toUpperCase(Locale.ROOT)); }
    private static void describe(List<String> out, String field, String current, FieldChange change) { if (change.action() == UpdateAction.KEEP) out.add(field + ": KEEP"); else if (change.action() == UpdateAction.REMOVE) out.add(field + ": REMOVE"); else out.add(field + ": " + (Objects.equals(current, change.value()) ? "KEEP" : "SET")); }
    private static void verify(TagEditPlan p, AudioMetadata m) { if (p.title().action() == UpdateAction.SET && !Objects.equals(p.title().value(), m.title())) throw new ProcessingException("VERIFY_FAILED", "VALIDATING", "Title verification failed"); if (p.artist().action() == UpdateAction.SET && !Objects.equals(p.artist().value(), m.artist())) throw new ProcessingException("VERIFY_FAILED", "VALIDATING", "Artist verification failed"); if (p.album().action() == UpdateAction.SET && !Objects.equals(p.album().value(), m.album())) throw new ProcessingException("VERIFY_FAILED", "VALIDATING", "Album verification failed"); if (p.lyrics().action() == UpdateAction.SET && !Objects.equals(p.lyrics().value(), m.lyrics())) throw new ProcessingException("VERIFY_FAILED", "VALIDATING", "Lyrics verification failed"); }
    private Path writeReport(long taskId, ResourceRecord r, TagEditPlan p, AudioMetadata m, VersionRecord v, Exception error) throws Exception { Path report = storage.reports().resolve("task-" + taskId + ".json"); Files.createDirectories(report.getParent()); Map<String,Object> doc = new LinkedHashMap<>(); doc.put("taskId", taskId); doc.put("resourceId", r.id()); doc.put("originalFilename", r.originalFilename()); doc.put("status", error == null ? "SUCCEEDED" : "FAILED"); doc.put("format", r.format()); if (v != null) { doc.put("versionId", v.id()); doc.put("outputFilename", v.outputPath().getFileName().toString()); doc.put("sha256", v.sha256()); } if (m != null) doc.put("verifiedMetadata", m); if (error != null) { doc.put("errorCode", error instanceof ProcessingException pe ? pe.code() : "INTERNAL_ERROR"); doc.put("errorMessage", error.getMessage()); } mapper.writerWithDefaultPrettyPrinter().writeValue(report.toFile(), doc); return report; }
    private void writeReportSafe(long id, ResourceRecord r, TagEditPlan p, AudioMetadata m, VersionRecord v, Exception e) { try { tasks.attachReport(id, writeReport(id, r, p, m, v, e)); } catch (Exception ignored) { } }
    private static void deleteTree(Path path) { try { if (Files.exists(path)) try (var s = Files.walk(path)) { s.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) { } }); } } catch (Exception ignored) { } }
}

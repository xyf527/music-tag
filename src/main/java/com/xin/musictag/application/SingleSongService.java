package com.xin.musictag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.domain.*;
import com.xin.musictag.tagging.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;

@Service
public class SingleSongService {
    @org.springframework.beans.factory.annotation.Autowired private StorageOperations operations;
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
        return process(id, plan, null, null);
    }
    public ProcessResult processBatch(long id, TagEditPlan plan, Path cover, long batchItemId) {
        var published = versions.forBatchItem(batchItemId);
        if (published.isPresent()) {
            VersionRecord version = published.get();
            return new ProcessResult(version.taskId(), version.id(), "SUCCEEDED", "/api/versions/" + version.id() + "/download", "/api/tasks/" + version.taskId() + "/report", null, null);
        }
        return process(id, plan, cover, batchItemId);
    }
    private ProcessResult process(long id, TagEditPlan plan, Path batchCover, Long batchItemId) {
        ResourceRecord resource = requireResource(id); AudioTagHandler handler = handlers.require(resource.format()); validateCapabilities(handler, plan);
        if (batchItemId != null) resource = new ResourceRecord(resource.id(), resource.originalFilename(), resource.format(), resource.byteSize(), resource.sha256(), resource.storagePath(), null, batchCover, resource.createdAt());
        VersionRecord priorVersion = versions.latestForResource(id).orElse(null);
        TaskRecord task = tasks.create(id); Path work = storage.working().resolve("task-" + task.id()); Path output;
        VersionRecord published = null;
        try {
            tasks.updateStatus(task.id(), "RUNNING", "COPYING", null, null); Files.createDirectories(work); Path working = work.resolve("working." + resource.format());
            Path source = priorVersion == null ? resource.storagePath() : priorVersion.outputPath();
            Files.copy(source, working, StandardCopyOption.REPLACE_EXISTING); output = storage.outputs().resolve("resource-" + id).resolve("version-" + UUID.randomUUID() + "." + resource.format());
            Files.createDirectories(output.getParent());
            AudioMetadata beforeWrite = handler.read(working);
            TagEditPlan resolved = resolveKeep(plan, beforeWrite, resource, priorVersion == null);
            TagWritePlan write = new TagWritePlan(resolved.title(), resolved.artist(), resolved.album(), resolved.lyrics(), resolved.artwork(), resource.coverPath());
            tasks.updateStatus(task.id(), "RUNNING", "WRITING", null, null); handler.write(working, write);
            tasks.updateStatus(task.id(), "RUNNING", "VALIDATING", null, null); AudioMetadata after = handler.read(working); verify(plan, after);
            String hash = AudioFileService.sha256(working);
            operations.preparePublication(task.id(),id,priorVersion == null ? null : priorVersion.id(),batchItemId,output,hash);
            Files.move(working, output, StandardCopyOption.ATOMIC_MOVE);
            Long parent = priorVersion == null ? null : priorVersion.id();
            VersionRecord version = batchItemId == null ? versions.create(id, parent, task.id(), output, hash) : versions.createForBatch(id, parent, task.id(), output, hash, batchItemId); published=version; tasks.attachVersion(task.id(), version.id()); tasks.updateStatus(task.id(), "SUCCEEDED", "PUBLISHED", null, null);
            Path report = writeReport(task.id(), resource, plan, after, version, null); tasks.attachReport(task.id(), report);
            // Queue errors cannot change the already published local result; startup reconciliation retries enqueue.
            try { operations.enqueue(version.id()); } catch (Exception ignored) { }
            operations.finishPublication(task.id());
            deleteTree(work); return new ProcessResult(task.id(), version.id(), "SUCCEEDED", "/api/versions/" + version.id() + "/download", "/api/tasks/" + task.id() + "/report", null, null);
        } catch (ProcessingException e) { if(published!=null) return publishedResult(published); tasks.updateStatus(task.id(), "FAILED", e.stage(), e.code(), e.getMessage()); writeReportSafe(task.id(), resource, plan, null, null, e); return new ProcessResult(task.id(), 0, "FAILED", null, "/api/tasks/" + task.id() + "/report", e.code(), e.getMessage());
        } catch (Exception e) { if(published!=null) return publishedResult(published); tasks.updateStatus(task.id(), "FAILED", "INTERNAL", "INTERNAL_ERROR", "Processing failed"); writeReportSafe(task.id(), resource, plan, null, null, e); return new ProcessResult(task.id(), 0, "FAILED", null, "/api/tasks/" + task.id() + "/report", "INTERNAL_ERROR", "Processing failed"); }
        finally { if(!Files.exists(storage.reports().resolve("task-"+task.id()+".publishing"))) deleteTree(work); }
    }
    private ProcessResult publishedResult(VersionRecord v) { return new ProcessResult(v.taskId(),v.id(),"SUCCEEDED","/api/versions/"+v.id()+"/download","/api/tasks/"+v.taskId()+"/report",null,null); }
    public TaskRecord task(long id) { return tasks.require(id); }
    public VersionRecord version(long id) { return versions.require(id); }
    public byte[] report(long id) { TaskRecord t = tasks.require(id); try { return Files.readAllBytes(secureRegularFile(Path.of(t.reportPath()), storage.reports())); } catch (Exception e) { throw new ProcessingException("FILE_IO_ERROR", "REPORT", "Report is unavailable"); } }
    public byte[] outputBytes(VersionRecord version) { try { return Files.readAllBytes(secureRegularFile(version.outputPath(), storage.outputs())); } catch (Exception e) { throw new ProcessingException("FILE_IO_ERROR", "DOWNLOAD", "Output is unavailable"); } }
    public void copyOutput(VersionRecord version, java.io.OutputStream output) throws IOException {
        try { Files.copy(secureRegularFile(version.outputPath(), storage.outputs()), output); }
        catch (Exception e) { throw new IOException("成品文件不可用"); }
    }
    private void validateCapabilities(AudioTagHandler h, TagEditPlan p) { if (!h.capabilities().lyrics() && p.lyrics().action() != UpdateAction.KEEP) throw new ProcessingException("UNSUPPORTED_FORMAT", "PREVIEW", "Lyrics operation is unsupported for " + h.format().toUpperCase(Locale.ROOT)); if (!h.capabilities().artwork() && p.artwork() != UpdateAction.KEEP) throw new ProcessingException("UNSUPPORTED_FORMAT", "PREVIEW", "Artwork operation is unsupported for " + h.format().toUpperCase(Locale.ROOT)); }
    private static void describe(List<String> out, String field, String current, FieldChange change) { if (change.action() == UpdateAction.KEEP) out.add(field + ": KEEP"); else if (change.action() == UpdateAction.REMOVE) out.add(field + ": REMOVE"); else out.add(field + ": " + (Objects.equals(current, change.value()) ? "KEEP" : "SET")); }
    private static TagEditPlan resolveKeep(TagEditPlan plan, AudioMetadata current, ResourceRecord resource, boolean firstVersion) throws IOException {
        FieldChange lyrics = resolve(plan.lyrics(), current.lyrics());
        UpdateAction artwork = plan.artwork();
        if (firstVersion && plan.lyrics().action() == UpdateAction.KEEP && resource.lyricsPath() != null) {
            lyrics = FieldChange.set(Files.readString(resource.lyricsPath()));
        }
        if (firstVersion && artwork == UpdateAction.KEEP && resource.coverPath() != null) artwork = UpdateAction.SET;
        return new TagEditPlan(resolve(plan.title(), current.title()), resolve(plan.artist(), current.artist()),
                resolve(plan.album(), current.album()), lyrics, artwork);
    }
    private static FieldChange resolve(FieldChange change, String current) {
        return change.action() == UpdateAction.KEEP ? (current == null ? FieldChange.remove() : FieldChange.set(current)) : change;
    }
    private static void verify(TagEditPlan p, AudioMetadata m) { if (p.title().action() == UpdateAction.SET && !Objects.equals(p.title().value(), m.title())) throw new ProcessingException("VERIFY_FAILED", "VALIDATING", "Title verification failed"); if (p.artist().action() == UpdateAction.SET && !Objects.equals(p.artist().value(), m.artist())) throw new ProcessingException("VERIFY_FAILED", "VALIDATING", "Artist verification failed"); if (p.album().action() == UpdateAction.SET && !Objects.equals(p.album().value(), m.album())) throw new ProcessingException("VERIFY_FAILED", "VALIDATING", "Album verification failed"); if (p.lyrics().action() == UpdateAction.SET && !Objects.equals(p.lyrics().value(), m.lyrics())) throw new ProcessingException("VERIFY_FAILED", "VALIDATING", "Lyrics verification failed"); }
    private Path writeReport(long taskId, ResourceRecord r, TagEditPlan p, AudioMetadata m, VersionRecord v, Exception error) throws Exception { Path report = storage.reports().resolve("task-" + taskId + ".json"); Files.createDirectories(report.getParent()); Map<String,Object> doc = new LinkedHashMap<>(); doc.put("taskId", taskId); doc.put("resourceId", r.id()); doc.put("originalFilename", r.originalFilename()); doc.put("status", error == null ? "SUCCEEDED" : "FAILED"); doc.put("format", r.format()); if (v != null) { doc.put("versionId", v.id()); doc.put("outputFilename", v.outputPath().getFileName().toString()); doc.put("sha256", v.sha256()); } if (m != null) doc.put("verifiedMetadata", m); if (error != null) { doc.put("errorCode", error instanceof ProcessingException pe ? pe.code() : "INTERNAL_ERROR"); doc.put("errorMessage", error.getMessage()); } mapper.writerWithDefaultPrettyPrinter().writeValue(report.toFile(), doc); return report; }
    private void writeReportSafe(long id, ResourceRecord r, TagEditPlan p, AudioMetadata m, VersionRecord v, Exception e) { try { tasks.attachReport(id, writeReport(id, r, p, m, v, e)); } catch (Exception ignored) { } }
    private static void deleteTree(Path path) { try { if (Files.exists(path)) try (var s = Files.walk(path)) { s.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) { } }); } } catch (Exception ignored) { } }
    private static Path secureRegularFile(Path candidate, Path root) throws Exception {
        if (candidate == null || root == null || Files.isSymbolicLink(candidate)) throw new IOException("Unsafe path");
        Path rootReal = root.toRealPath();
        Path candidateReal = candidate.toRealPath();
        if (!candidateReal.startsWith(rootReal) || !Files.isRegularFile(candidateReal, java.nio.file.LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe path");
        return candidateReal;
    }
}

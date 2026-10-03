package com.xin.musictag.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.domain.BatchItem;
import com.xin.musictag.domain.BatchRepository;
import com.xin.musictag.domain.BatchTask;
import com.xin.musictag.domain.ProcessResult;
import com.xin.musictag.domain.ResourceRecord;
import com.xin.musictag.domain.TaskRepository;
import com.xin.musictag.domain.VersionRecord;
import com.xin.musictag.domain.VersionRepository;
import com.xin.musictag.domain.TagEditPlan;
import com.xin.musictag.tagging.FieldChange;
import com.xin.musictag.tagging.UpdateAction;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BatchImportService {
    private final BatchRepository batches;
    private final AudioFileService uploads;
    private final SingleSongService songs;
    private final VersionRepository versions;
    private final ObjectMapper objectMapper;
    private final TaskExecutor batchExecutor;
    private final int concurrency;

    public BatchImportService(BatchRepository batches, AudioFileService uploads, SingleSongService songs,
                              VersionRepository versions, ObjectMapper objectMapper, TaskExecutor batchExecutor,
                              @Value("${MUSIC_BATCH_CONCURRENCY:2}") int concurrency) {
        this.batches = batches;
        this.uploads = uploads;
        this.songs = songs;
        this.versions = versions;
        this.objectMapper = objectMapper;
        this.batchExecutor = batchExecutor;
        this.concurrency = Math.max(1, Math.min(concurrency, 4));
    }

    @PostConstruct
    void recoverInterruptedRuns() {
        batches.recoverInterrupted();
    }

    public BatchTask importFiles(List<MultipartFile> files, List<String> paths) {
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("请选择至少一个文件");
        if (paths == null || paths.size() != files.size()) throw new IllegalArgumentException("文件路径信息不完整");
        List<FilePart> parts = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) parts.add(new FilePart(files.get(i), normalPath(paths.get(i))));
        BatchTask task = batches.create();
        Map<String, List<FilePart>> lyrics = byKey(parts, "lrc");
        Map<String, List<FilePart>> covers = byKey(parts, "cover");
        for (FilePart part : parts) {
            String extension = ext(part.path());
            if (audio(extension)) {
                List<FilePart> lrc = lyrics.getOrDefault(key(part.path()), List.of());
                List<FilePart> cover = covers.getOrDefault(key(part.path()), List.of());
                boolean conflict = lrc.size() > 1 || cover.size() > 1;
                MultipartFile matchedLrc = lrc.size() == 1 ? lrc.get(0).file() : null;
                MultipartFile matchedCover = cover.size() == 1 ? cover.get(0).file() : null;
                ResourceRecord resource = uploads.upload(part.file(), matchedLrc, matchedCover);
                String status = conflict ? "CONFLICT" : "PENDING";
                String basis = conflict ? "COLLISION" : (matchedLrc != null || matchedCover != null ? "RELATIVE_PATH_AND_STEM" : "UNBOUND");
                String warning = conflict ? "同目录同名的歌词或封面不唯一，请手工处理" : null;
                batches.add(task.id(), resource.id(), part.path(), "AUDIO", status, basis,
                        matchedLrc == null ? null : name(matchedLrc), matchedCover == null ? null : name(matchedCover), warning);
            } else if ("lrc".equals(extension) || cover(extension)) {
                batches.add(task.id(), null, part.path(), "lrc".equals(extension) ? "LYRICS" : "COVER", "UNBOUND", "UNBOUND", null, null, null);
            } else {
                batches.add(task.id(), null, part.path(), "UNSUPPORTED", "UNSUPPORTED", "UNSUPPORTED", null, null, "不支持的文件类型");
            }
        }
        return batches.require(task.id());
    }

    public BatchTask confirm(long taskId, Set<Long> skipIds) {
        List<BatchItem> items = batches.items(taskId);
        for (BatchItem item : items) if (skipIds.contains(item.id()) && "AUDIO".equals(item.kind())) batches.itemStatus(item.id(), "SKIPPED", "SKIPPED", null, null, "用户跳过");
        List<BatchItem> refreshed = batches.items(taskId);
        boolean unresolved = refreshed.stream().anyMatch(item -> "AUDIO".equals(item.kind()) && "CONFLICT".equals(item.status()));
        if (unresolved) throw new IllegalArgumentException("存在匹配冲突，请先手工解绑或跳过冲突音频");
        try {
            batches.plan(taskId, objectMapper.writeValueAsString(refreshed));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法生成处理计划", exception);
        }
        return batches.require(taskId);
    }

    public void execute(long taskId, boolean retryOnly) {
        BatchTask task = batches.require(taskId);
        if (!retryOnly && !"CONFIRMED".equals(task.status())) throw new IllegalArgumentException("请先确认不可变处理计划");
        batches.taskStatus(taskId, "RUNNING");
        batchExecutor.execute(() -> run(taskId, retryOnly));
    }

    private void run(long taskId, boolean retryOnly) {
        List<BatchItem> items = batches.items(taskId);
        for (BatchItem item : items) {
            boolean eligible = "AUDIO".equals(item.kind()) && (retryOnly ? "FAILED".equals(item.status()) : "PENDING".equals(item.status()));
            if (!eligible) continue;
            try {
                batches.itemStatus(item.id(), "RUNNING", "WRITE", null, null, null);
                ProcessResult result = songs.process(item.resourceId(), new TagEditPlan(
                        FieldChange.keep(), FieldChange.keep(), FieldChange.keep(), FieldChange.keep(), UpdateAction.KEEP));
                batches.itemStatus(item.id(), "SUCCESS", "DONE", result.versionId(), null, null);
            } catch (RuntimeException exception) {
                batches.itemStatus(item.id(), "FAILED", "FAILED", null, "PROCESSING_FAILED", "处理失败，请重试");
            }
        }
        boolean failed = batches.items(taskId).stream().anyMatch(item -> "FAILED".equals(item.status()));
        batches.taskStatus(taskId, failed ? "COMPLETED_WITH_FAILURES" : "COMPLETED");
    }

    public Map<String, Object> detail(long taskId) {
        BatchTask task = batches.require(taskId);
        List<BatchItem> items = batches.items(taskId);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (BatchItem item : items) counts.merge(item.status(), 1L, Long::sum);
        return Map.of("task", task, "items", items, "counts", counts, "concurrency", concurrency);
    }

    public byte[] zip(long taskId) {
        Map<String, Object> report = detail(taskId);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (BatchItem item : batches.items(taskId)) {
                if (!"SUCCESS".equals(item.status()) || item.outputVersionId() == null) continue;
                VersionRecord version = versions.require(item.outputVersionId());
                String filename = version.outputPath().getFileName().toString();
                String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                zip.putNextEntry(new ZipEntry("outputs/item-" + item.id() + "." + extension));
                zip.write(songs.outputBytes(version));
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("report.json"));
            zip.write(objectMapper.writeValueAsBytes(report));
            zip.closeEntry();
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("无法生成下载包", exception);
        }
    }

    private static Map<String, List<FilePart>> byKey(List<FilePart> parts, String type) {
        Map<String, List<FilePart>> found = new LinkedHashMap<>();
        for (FilePart part : parts) if (("lrc".equals(type) && "lrc".equals(ext(part.path()))) || ("cover".equals(type) && cover(ext(part.path()))))
            found.computeIfAbsent(key(part.path()), unused -> new ArrayList<>()).add(part);
        return found;
    }
    private static boolean audio(String ext) { return Set.of("mp3", "flac", "wav").contains(ext); }
    private static boolean cover(String ext) { return Set.of("jpg", "jpeg", "png").contains(ext); }
    private static String ext(String path) { int dot = path.lastIndexOf('.'); return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT); }
    private static String key(String path) { int dot = path.lastIndexOf('.'); return (dot < 0 ? path : path.substring(0, dot)).toLowerCase(Locale.ROOT); }
    private static String name(MultipartFile file) { return file.getOriginalFilename() == null ? "" : file.getOriginalFilename(); }
    private static String normalPath(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("文件相对路径不能为空");
        String path = raw.replace('\\', '/').replaceAll("/+", "/");
        if (path.startsWith("/") || path.contains("../") || path.equals("..")) throw new IllegalArgumentException("文件路径不安全");
        return path;
    }
    private record FilePart(MultipartFile file, String path) { }
}

package com.xin.musictag.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.domain.*;
import com.xin.musictag.tagging.FieldChange;
import com.xin.musictag.tagging.UpdateAction;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BatchImportService {
    private final BatchRepository batches;
    private final AudioFileService uploads;
    private final SingleSongService songs;
    private final VersionRepository versions;
    private final ObjectMapper mapper;
    private final TaskExecutor executor;
    private final int concurrency;
    private final UploadLimits limits;
    private final StorageSettings storage;

    public BatchImportService(BatchRepository batches, AudioFileService uploads, SingleSongService songs,
                              VersionRepository versions, ObjectMapper mapper, TaskExecutor batchExecutor,
                              @Value("${MUSIC_BATCH_CONCURRENCY:2}") int concurrency, UploadLimits limits, StorageSettings storage) {
        this.batches = batches; this.uploads = uploads; this.songs = songs; this.versions = versions;
        this.mapper = mapper; this.executor = batchExecutor; this.concurrency = Math.max(1, Math.min(concurrency, 4));
        this.limits = limits; this.storage = storage;
    }

    @PostConstruct public void recoverInterruptedRuns() { batches.recoverInterrupted(); }

    public Map<String,Object> policy() {
        return Map.of("maxFiles", limits.maxBatchFiles(), "maxBytes", limits.maxBatchBytes(),
                "maxAudioBytes", limits.maxAudioBytes(), "concurrency", concurrency,"capabilities",uploads.capabilities(),"formats",uploads.outputFormats());
    }

    @Transactional
    public BatchTask importFiles(List<MultipartFile> files, List<String> paths) {
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("请选择至少一个文件");
        if (files.size() > limits.maxBatchFiles()) throw new IllegalArgumentException("单批文件数量超过上限 " + limits.maxBatchFiles());
        long total = 0;
        for (MultipartFile file : files) {
            if (file.isEmpty()) throw new IllegalArgumentException("不能上传空文件");
            if (file.getSize() > limits.maxBatchBytes() - total) throw new IllegalArgumentException("单批总大小超过上限 " + limits.maxBatchBytes() + " 字节");
            total += file.getSize();
        }
        if (paths == null || paths.size() != files.size()) throw new IllegalArgumentException("文件路径信息不完整");
        List<String> safePaths = paths.stream().map(BatchImportService::normalPath).toList();
        List<MultipartFile> namedFiles = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index); String original = file.getOriginalFilename();
            if (original == null) throw new IllegalArgumentException("文件名不能为空");
            if (original.contains("/") || original.contains("\\")) {
                if (!normalPath(original).equals(safePaths.get(index))) throw new IllegalArgumentException("上传目录与相对路径不一致");
                original = original.replace('\\','/'); original = original.substring(original.lastIndexOf('/') + 1);
            }
            try { namedFiles.add(new NamedFile(file, AudioFileService.safeFilename(original))); }
            catch (ProcessingException exception) { throw new IllegalArgumentException("文件名不安全，请检查后重新选择"); }
        }
        uploads.requireUploadSpace();
        BatchTask task = batches.create();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = namedFiles.get(index); String path = safePaths.get(index), extension = ext(path);
            if (!extension.equals(ext(file.getOriginalFilename()))) throw new IllegalArgumentException("文件路径与扩展名不一致");
            String kind = uploads.supportsOutput(extension) ? "AUDIO" : extension.equals("lrc") ? "LYRICS" : cover(extension) ? "COVER" : "UNSUPPORTED";
            try {
                if ("AUDIO".equals(kind)) {
                    ResourceRecord resource = uploads.upload(file, null, null);
                    BatchItem item = batches.add(task.id(), resource.id(), path, kind, "PENDING", "UNBOUND", null, null, null);
                    var metadata = songs.metadata(resource.id());
                    batches.metadata(item.id(), metadata.title(), metadata.artist());
                } else if (!"UNSUPPORTED".equals(kind)) {
                    BatchItem item = batches.add(task.id(), null, path, kind, "UNBOUND", "UNBOUND", null, null, null);
                    Path folder = assetRoot().resolve(Long.toString(item.id()));
                    Files.createDirectories(folder);
                    Path saved = kind.equals("LYRICS") ? uploads.copyLyrics(file, folder) : uploads.copyCover(file, folder);
                    String text = kind.equals("LYRICS") ? Files.readString(saved) : null;
                    batches.attachment(new BatchAttachment(item.id(), saved, AudioFileService.sha256(saved), text, lrcTag(text, "ti"), lrcTag(text, "ar")));
                } else {var unsupported=batches.add(task.id(), null, path, kind, "UNSUPPORTED", "UNSUPPORTED", null, null, "不支持的文件类型（UNSUPPORTED）");batches.itemStatus(unsupported.id(),"UNSUPPORTED","UNSUPPORTED",null,"UNSUPPORTED_FORMAT","当前格式不支持处理（UNSUPPORTED）");}
            } catch (Exception exception) {
                // A damaged input is a visible item; it does not discard the other imported files.
                BatchItem item = batches.items(task.id()).stream().filter(i -> i.relativePath().equals(path)).reduce((a,b) -> b).orElse(null);
                String code=exception instanceof ProcessingException p?p.code():"INVALID_FILE";
                if (item == null) item = batches.add(task.id(), null, path, kind, "FAILED", "INVALID_FILE", null, null, "文件损坏或格式不受支持，请检查原文件");
                batches.itemStatus(item.id(), "FAILED", "UPLOAD", null, code, "文件损坏或格式不受支持，请检查原文件");
            }
        }
        autoMatch(task.id());
        return batches.require(task.id());
    }

    private void autoMatch(long taskId) {
        List<BatchItem> all = batches.items(taskId);
        List<BatchItem> audios = all.stream().filter(i -> i.kind().equals("AUDIO") && i.resourceId() != null).toList();
        List<BatchItem> assets = all.stream().filter(i -> Set.of("LYRICS","COVER").contains(i.kind()) && !i.status().equals("FAILED")).toList();
        Map<Long,List<Long>> lyricCandidates = new LinkedHashMap<>(), coverCandidates = new LinkedHashMap<>();
        Map<Long,String> bases = new HashMap<>();
        Set<Long> tagConflicts = new HashSet<>();
        for (BatchItem audio : audios) {
            List<BatchItem> exactLyrics = assets.stream().filter(a -> a.kind().equals("LYRICS") && key(a.relativePath()).equals(key(audio.relativePath()))).toList();
            List<BatchItem> selected = exactLyrics;
            String basis = "RELATIVE_PATH_AND_STEM";
            if (exactLyrics.isEmpty()) {
                selected = assets.stream().filter(a -> a.kind().equals("LYRICS") && tagMatches(audio, batches.attachment(a.id()))).toList();
                basis = selected.isEmpty() ? "UNBOUND" : "LRC_TITLE_ARTIST";
            } else {
                for (BatchItem a : exactLyrics) {
                    BatchAttachment attachment = batches.attachment(a.id());
                    if (ambiguousTag(attachment.lyricsText(),"ti") || ambiguousTag(attachment.lyricsText(),"ar")) tagConflicts.add(audio.id());
                    if (hasTags(audio) && attachment.title() != null && attachment.artist() != null && !tagMatches(audio, attachment)) tagConflicts.add(audio.id());
                }
            }
            // Same basename in another directory is a warning requiring an explicit choice.
            if (selected.isEmpty()) {
                selected = assets.stream().filter(a -> a.kind().equals("LYRICS") && stem(a.relativePath()).equals(stem(audio.relativePath()))).toList();
                if (!selected.isEmpty()) { tagConflicts.add(audio.id()); basis = "DIRECTORY_CONFLICT"; }
            }
            List<BatchItem> covers = assets.stream().filter(a -> a.kind().equals("COVER") && key(a.relativePath()).equals(key(audio.relativePath()))).toList();
            if (covers.isEmpty()) {
                covers = assets.stream().filter(a -> a.kind().equals("COVER") && stem(a.relativePath()).equals(stem(audio.relativePath()))).toList();
                if (!covers.isEmpty()) tagConflicts.add(audio.id());
            }
            lyricCandidates.put(audio.id(), selected.stream().map(BatchItem::id).toList());
            coverCandidates.put(audio.id(), covers.stream().map(BatchItem::id).toList());
            if (basis.equals("UNBOUND") && !covers.isEmpty()) basis = "RELATIVE_PATH_AND_STEM";
            bases.put(audio.id(), basis);
        }
        Map<Long,Long> uses = new HashMap<>();
        for (var candidates : List.of(lyricCandidates, coverCandidates)) candidates.values().forEach(ids -> ids.forEach(id -> uses.merge(id, 1L, Long::sum)));
        for (BatchItem audio : audios) {
            List<Long> lyrics = lyricCandidates.get(audio.id()), covers = coverCandidates.get(audio.id());
            boolean conflict = tagConflicts.contains(audio.id()) || lyrics.size() > 1 || covers.size() > 1 ||
                    lyrics.stream().anyMatch(id -> uses.get(id) > 1) || covers.stream().anyMatch(id -> uses.get(id) > 1);
            Long lyricId = !conflict && lyrics.size() == 1 ? lyrics.get(0) : null;
            Long coverId = !conflict && covers.size() == 1 ? covers.get(0) : null;
            batches.match(audio.id(), lyricId, coverId, conflict ? "CONFLICT" : "PENDING",
                    conflict ? "COLLISION" : bases.get(audio.id()), json(Map.of("lyrics", lyrics, "covers", covers)),
                    conflict ? "存在多候选、标签冲突或重复绑定，请人工选择或解绑" : null);
        }
        refreshAssets(taskId);
    }

    @Transactional
    public void bind(long taskId, long audioId, Long lyricsId, Long coverId) {
        batches.lockDraft(taskId);
        List<BatchItem> items = batches.items(taskId);
        BatchItem audio = item(items, audioId);
        if (!audio.kind().equals("AUDIO") || audio.resourceId() == null) throw new IllegalArgumentException("请选择有效的音频项目");
        requireAttachments(audio,lyricsId!=null,coverId!=null);
        validateBinding(items, audioId, lyricsId, "LYRICS");
        validateBinding(items, audioId, coverId, "COVER");
        batches.match(audioId, lyricsId, coverId, "PENDING", "MANUAL", audio.candidatesJson(), null);
        refreshAssets(taskId);
    }
    private static void validateBinding(List<BatchItem> items, long audioId, Long assetId, String kind) {
        if (assetId == null) return;
        BatchItem asset = item(items, assetId);
        if (!asset.kind().equals(kind) || asset.status().equals("FAILED")) throw new IllegalArgumentException("附件类型不正确或附件损坏");
        boolean used = items.stream().anyMatch(i -> i.id() != audioId && Objects.equals(assetId, kind.equals("LYRICS") ? i.lyricsItemId() : i.coverItemId()));
        if (used) throw new IllegalArgumentException("附件已经绑定到另一首音频，请先解绑");
    }
    private void refreshAssets(long taskId) {
        List<BatchItem> items = batches.items(taskId);
        Set<Long> bound = new HashSet<>();
        items.forEach(i -> { if (i.lyricsItemId() != null) bound.add(i.lyricsItemId()); if (i.coverItemId() != null) bound.add(i.coverItemId()); });
        for (BatchItem item : items) if (Set.of("LYRICS","COVER").contains(item.kind()) && !item.status().equals("FAILED"))
            batches.itemStatus(item.id(), bound.contains(item.id()) ? "BOUND" : "UNBOUND", "MATCHED", null, null, null);
    }

    @Transactional
    public BatchTask confirm(long taskId, Set<Long> skipIds) {
        batches.lockDraft(taskId);
        List<BatchItem> items = batches.items(taskId);
        for (Long id : skipIds) if (!item(items, id).kind().equals("AUDIO")) throw new IllegalArgumentException("只能跳过音频项目");
        List<ProcessingPlan.Entry> entries = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (BatchItem item : items) {
            if (!item.kind().equals("AUDIO") || item.resourceId() == null) continue;
            boolean skip = skipIds.contains(item.id());
            if(!skip)requireAttachments(item,item.lyricsItemId()!=null,item.coverItemId()!=null);
            if (!skip && item.status().equals("CONFLICT")) throw new IllegalArgumentException("存在匹配冲突，请先人工绑定、解绑或跳过");
            for (Long id : Arrays.asList(item.lyricsItemId(), item.coverItemId())) if (!skip && id != null && !used.add(id)) throw new IllegalArgumentException("附件重复绑定");
            BatchAttachment lyrics = item.lyricsItemId() == null ? null : batches.attachment(item.lyricsItemId());
            BatchAttachment cover = item.coverItemId() == null ? null : batches.attachment(item.coverItemId());
            if (!skip && cover != null) secureAsset(cover, cover.sha256());
            entries.add(new ProcessingPlan.Entry(item.id(), item.resourceId(), item.relativePath(), skip,
                    lyrics == null ? null : lyrics.lyricsText(), cover == null ? null : cover.itemId(), cover == null ? null : cover.sha256()));
            if (skip) batches.itemStatus(item.id(), "SKIPPED", "SKIPPED", null, null, "用户跳过");
        }
        batches.plan(taskId, json(new ProcessingPlan(1, taskId, List.copyOf(entries))));
        return batches.require(taskId);
    }

    public void execute(long taskId, boolean retryOnly) {
        ProcessingPlan plan = readPlan(batches.require(taskId));
        Set<String> allowedTask = retryOnly ? Set.of("COMPLETED_WITH_FAILURES","INTERRUPTED","COMPLETED") : Set.of("CONFIRMED");
        if (!batches.start(taskId, allowedTask)) throw new IllegalArgumentException("任务正在执行或尚未确认");
        Set<String> eligible = retryOnly ? Set.of("FAILED","INTERRUPTED") : Set.of("PENDING");
        Map<Long,BatchItem> current = new HashMap<>(); batches.items(taskId).forEach(i -> current.put(i.id(), i));
        List<ProcessingPlan.Entry> pending = plan.entries().stream().filter(e -> !e.skip() && eligible.contains(current.get(e.itemId()).status()) && current.get(e.itemId()).outputVersionId() == null).toList();
        if (pending.isEmpty()) { finish(taskId); return; }
        int workers = Math.min(concurrency, pending.size());
        AtomicInteger cursor = new AtomicInteger(), remaining = new AtomicInteger(workers);
        for (int i = 0; i < workers; i++) {
            try {
                executor.execute(() -> {
                    try {
                        for (int index; (index = cursor.getAndIncrement()) < pending.size();) {
                            ProcessingPlan.Entry entry = pending.get(index);
                            if (batches.claim(entry.itemId(), eligible)) process(entry);
                        }
                    } finally { if (remaining.decrementAndGet() == 0) finish(taskId); }
                });
            } catch (RuntimeException rejected) {
                if (remaining.decrementAndGet() == 0) finish(taskId);
            }
        }
    }
    private void process(ProcessingPlan.Entry entry) {
        try {
            Path cover = entry.coverItemId() == null ? null : secureAsset(batches.attachment(entry.coverItemId()), entry.coverSha256());
            TagEditPlan edit = new TagEditPlan(FieldChange.keep(), FieldChange.keep(), FieldChange.keep(),
                    entry.lyricsText() == null ? FieldChange.keep() : FieldChange.set(entry.lyricsText()), cover == null ? UpdateAction.KEEP : UpdateAction.SET);
            ProcessResult result = songs.processBatch(entry.resourceId(), edit, cover, entry.itemId());
            if (!"SUCCEEDED".equals(result.status()) || result.versionId() <= 0) {
                batches.itemStatus(entry.itemId(), "FAILED", "FAILED", null, result.errorCode() == null ? "PROCESSING_FAILED" : result.errorCode(), "处理失败，请检查音频格式或重试");
            } else batches.itemStatus(entry.itemId(), "SUCCESS", "DONE", result.versionId(), null, null);
        } catch (RuntimeException exception) {
            batches.itemStatus(entry.itemId(), "FAILED", "FAILED", null, exception instanceof ProcessingException p ? p.code() : "PROCESSING_FAILED", "处理失败，请检查附件或原文件后重试");
        }
    }
    private void finish(long taskId) {
        List<BatchItem> items = batches.items(taskId);
        boolean waiting = items.stream().anyMatch(i -> i.kind().equals("AUDIO") && Set.of("PENDING","RUNNING","INTERRUPTED").contains(i.status()));
        boolean failed = items.stream().anyMatch(i -> i.status().equals("FAILED"));
        batches.taskStatus(taskId, waiting ? "INTERRUPTED" : failed ? "COMPLETED_WITH_FAILURES" : "COMPLETED");
    }
    private ProcessingPlan readPlan(BatchTask task) {
        try {
            ProcessingPlan plan = mapper.readValue(task.planJson(), ProcessingPlan.class);
            if (plan.schemaVersion() != 1 || plan.taskId() != task.id()) throw new IllegalArgumentException();
            return plan;
        } catch (Exception exception) { throw new IllegalArgumentException("处理计划不存在或版本不兼容，请重新导入"); }
    }

    public Map<String,Object> detail(long taskId) {
        BatchTask task = batches.require(taskId); List<BatchItem> items = batches.items(taskId);
        Map<String,Long> counts = new LinkedHashMap<>(), kinds = new LinkedHashMap<>();
        items.forEach(i -> { counts.merge(i.status(), 1L, Long::sum); kinds.merge(i.kind(), 1L, Long::sum); });
        var capabilities=new LinkedHashMap<String,Object>();var outputs=new ArrayList<Map<String,Object>>();
        for(BatchItem item:items)if(item.resourceId()!=null){var resource=songs.requireResource(item.resourceId());capabilities.put(Long.toString(item.id()),uploads.capabilities().get(resource.format()));
            if(item.outputVersionId()!=null){var version=versions.require(item.outputVersionId());outputs.add(Map.of("itemId",item.id(),"versionId",version.id(),"taskId",version.taskId(),"format",resource.format(),"sha256",version.sha256(),"verification","WRITE_REREAD_PASSED","reportUrl","/api/tasks/"+version.taskId()+"/report"));}}
        return Map.of("task", Map.of("id", task.id(), "status", task.status(), "confirmed", task.planJson() != null, "createdAt", task.createdAt()),
                "items", items, "counts", counts, "kinds", kinds, "total", items.size(), "concurrency", concurrency,"itemCapabilities",capabilities,"outputs",outputs);
    }
    private void requireAttachments(BatchItem item,boolean lyrics,boolean cover){var capabilities=uploads.capabilities().get(songs.requireResource(item.resourceId()).format());capabilities.require(new com.xin.musictag.tagging.TagWritePlan(FieldChange.keep(),FieldChange.keep(),FieldChange.keep(),lyrics?FieldChange.set("attachment"):FieldChange.keep(),cover?UpdateAction.SET:UpdateAction.KEEP,null));}
    public void writeZip(long taskId, OutputStream output) throws IOException {
        assertDownloadable(taskId);
        Map<String,Object> report = detail(taskId);
        @SuppressWarnings("unchecked") List<BatchItem> items = (List<BatchItem>) report.get("items");
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
        for (BatchItem item : items) {
            if (!"SUCCESS".equals(item.status()) || item.outputVersionId() == null) continue;
            VersionRecord version = versions.require(item.outputVersionId());
            String extension = ext(version.outputPath().getFileName().toString());
            if (!uploads.supportsOutput(extension)) throw new IOException("成品格式无效");
            ZipEntry entry = new ZipEntry("outputs/item-" + item.id() + "." + extension); entry.setTime(0);
            zip.putNextEntry(entry); songs.copyOutput(version, zip); zip.closeEntry();
        }
        ZipEntry reportEntry = new ZipEntry("report.json"); reportEntry.setTime(0); zip.putNextEntry(reportEntry);
        zip.write(mapper.writeValueAsBytes(report)); zip.closeEntry(); zip.finish(); zip.flush();
        }
    }
    public void assertDownloadable(long taskId) {
        if (!Set.of("COMPLETED","COMPLETED_WITH_FAILURES","INTERRUPTED").contains(batches.require(taskId).status())) throw new IllegalArgumentException("任务执行后才能下载");
    }
    private Path assetRoot() { return storage.uploads().resolve("batch-attachments"); }
    private Path secureAsset(BatchAttachment asset, String expectedHash) {
        try {
            Path root = assetRoot().toRealPath(), path = asset.path().toRealPath();
            if (!path.startsWith(root) || Files.isSymbolicLink(asset.path()) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) ||
                    !Objects.equals(expectedHash, AudioFileService.sha256(path))) throw new IOException();
            return path;
        } catch (Exception exception) { throw new IllegalArgumentException("附件已损坏或路径不安全"); }
    }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("无法保存处理计划"); } }
    private static BatchItem item(List<BatchItem> items, long id) { return items.stream().filter(i -> i.id() == id).findFirst().orElseThrow(() -> new IllegalArgumentException("项目不属于本批次")); }
    private static boolean hasTags(BatchItem audio) { return audio.title() != null && !audio.title().isBlank() && audio.artist() != null && !audio.artist().isBlank(); }
    private static boolean tagMatches(BatchItem audio, BatchAttachment lyrics) {
        return hasTags(audio) && lyrics.title() != null && lyrics.artist() != null &&
                normalize(audio.title()).equals(normalize(lyrics.title())) && normalize(audio.artist()).equals(normalize(lyrics.artist()));
    }
    private static String normalize(String value) { return Normalizer.normalize(value.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT); }
    private static String lrcTag(String text, String tag) {
        Set<String> values = lrcValues(text,tag);
        return values.size() == 1 ? values.iterator().next() : null;
    }
    private static boolean ambiguousTag(String text,String tag) { return lrcValues(text,tag).size() > 1; }
    private static Set<String> lrcValues(String text,String tag) {
        Set<String> values = new LinkedHashSet<>();
        if (text == null) return values;
        var matcher = Pattern.compile("\\[" + tag + ":([^\\]\\r\\n]*)\\]", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) if (!matcher.group(1).isBlank()) values.add(normalize(matcher.group(1)));
        return values;
    }
    private static boolean cover(String extension) { return Set.of("jpg","jpeg","png").contains(extension); }
    private static String ext(String path) { int dot = path.lastIndexOf('.'); return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT); }
    private static String key(String path) { int dot = path.lastIndexOf('.'); return normalize(dot < 0 ? path : path.substring(0, dot)); }
    private static String stem(String path) { return key(path.substring(path.lastIndexOf('/') + 1)); }
    private static String normalPath(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 1000 || raw.indexOf(0) >= 0) throw new IllegalArgumentException("文件相对路径无效");
        String path = raw.replace('\\', '/');
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") || Arrays.stream(path.split("/", -1)).anyMatch(p -> p.equals("..") || p.equals(".") || p.isBlank())) throw new IllegalArgumentException("文件路径不安全");
        return path;
    }
    private record NamedFile(MultipartFile delegate, String filename) implements MultipartFile {
        public String getName() { return delegate.getName(); }
        public String getOriginalFilename() { return filename; }
        public String getContentType() { return delegate.getContentType(); }
        public boolean isEmpty() { return delegate.isEmpty(); }
        public long getSize() { return delegate.getSize(); }
        public byte[] getBytes() throws IOException { return delegate.getBytes(); }
        public InputStream getInputStream() throws IOException { return delegate.getInputStream(); }
        public void transferTo(File destination) throws IOException { delegate.transferTo(destination); }
        public void transferTo(Path destination) throws IOException { delegate.transferTo(destination); }
    }
}

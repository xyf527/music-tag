package com.xin.musictag.application;

import com.xin.musictag.domain.ProcessingException;
import com.xin.musictag.domain.ResourceRecord;
import com.xin.musictag.domain.ResourceRepository;
import com.xin.musictag.domain.StorageSettings;
import com.xin.musictag.tagging.AudioMetadata;
import com.xin.musictag.tagging.AudioTagHandler;
import com.xin.musictag.tagging.AudioTagHandlerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AudioFileService {
    private final StorageSettings storage;
    private final UploadLimits limits;
    private final ResourceRepository resources;
    private final AudioTagHandlerRegistry handlers;

    public AudioFileService(StorageSettings storage, UploadLimits limits, ResourceRepository resources, AudioTagHandlerRegistry handlers) {
        this.storage = storage; this.limits = limits; this.resources = resources; this.handlers = handlers;
    }

    public ResourceRecord upload(MultipartFile audio, MultipartFile lyrics, MultipartFile cover) {
        if (audio == null || audio.isEmpty()) throw new ProcessingException("FILE_IO_ERROR", "UPLOAD", "音频文件为空，请选择 MP3、FLAC 或 WAV 文件");
        if (audio.getSize() > limits.maxAudioBytes()) throw new ProcessingException("UPLOAD_TOO_LARGE", "UPLOAD", "音频文件超过允许的大小上限 " + limits.maxAudioMegabytes());
        String filename = safeFilename(audio.getOriginalFilename());
        String extension = extension(filename);
        if (!extension.matches("mp3|flac|wav")) throw new ProcessingException("UNSUPPORTED_FORMAT", "UPLOAD", "只支持 MP3、FLAC 和 WAV 音频格式");
        Path folder = storage.uploads().resolve(UUID.randomUUID().toString());
        Path original = folder.resolve("original." + extension);
        try {
            Files.createDirectories(folder);
            audio.transferTo(original);
            verifyMagic(original, extension);
            AudioTagHandler handler = handlers.require(extension);
            handler.read(original);
            Path lyricPath = copyLyrics(lyrics, folder);
            Path coverPath = copyCover(cover, folder);
            return resources.create(filename, extension, Files.size(original), sha256(original), original, lyricPath, coverPath);
        } catch (ProcessingException e) {
            deleteQuietly(folder); throw e;
        } catch (Exception e) {
            deleteQuietly(folder); throw new ProcessingException("CORRUPT_AUDIO", "UPLOAD", "Audio could not be parsed");
        }
    }

    public long maxAudioBytes() { return limits.maxAudioBytes(); }
    public long maxRequestBytes() { return limits.maxRequestBytes(); }
    public String maxAudioMegabytes() { return limits.maxAudioMegabytes(); }

    private Path copyLyrics(MultipartFile file, Path folder) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (!extension(safeFilename(file.getOriginalFilename())).equals("lrc")) throw new ProcessingException("INVALID_LRC", "UPLOAD", "歌词文件必须是 .lrc 格式");
        if (file.getSize() > 2 * 1024 * 1024) throw new ProcessingException("INVALID_LRC", "UPLOAD", "LRC 文件超过 2 MB 大小限制");
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (!text.isBlank() && !text.contains("[")) throw new ProcessingException("INVALID_LRC", "UPLOAD", "LRC 文件必须包含时间戳或元数据");
        Path target = folder.resolve("lyrics.lrc"); Files.writeString(target, text, StandardCharsets.UTF_8); return target;
    }
    private Path copyCover(MultipartFile file, Path folder) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String name = safeFilename(file.getOriginalFilename());
        if (!extension(name).matches("jpg|jpeg|png")) throw new ProcessingException("INVALID_COVER", "UPLOAD", "封面必须是 JPG 或 PNG 图片");
        if (file.getSize() > 10 * 1024 * 1024) throw new ProcessingException("INVALID_COVER", "UPLOAD", "封面文件超过 10 MB 大小限制");
        Path target = folder.resolve("cover"); Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        if (ImageIO.read(target.toFile()) == null) throw new ProcessingException("INVALID_COVER", "UPLOAD", "封面不是可读取的 JPG 或 PNG 图片");
        return target;
    }
    private static void verifyMagic(Path file, String extension) throws IOException {
        byte[] bytes = new byte[12]; int count;
        try (InputStream input = Files.newInputStream(file)) { count = input.read(bytes); }
        if (count < 0) count = 0;
        String actual;
        if (extension.equals("flac") && count >= 4 && bytes[0] == 'f' && bytes[1] == 'L' && bytes[2] == 'a' && bytes[3] == 'C') actual = "flac";
        else if (extension.equals("wav") && count >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E') actual = "wav";
        else if (extension.equals("mp3") && count >= 3 && ((bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') || (bytes[0] == (byte) 0xff && (bytes[1] & 0xe0) == 0xe0))) actual = "mp3";
        else actual = "unknown";
        if (!actual.equals(extension)) throw new ProcessingException("CORRUPT_AUDIO", "UPLOAD", "文件扩展名与实际音频格式不一致");
    }
    static String safeFilename(String value) {
        if (value == null || value.isBlank() || value.contains("\0") || value.contains("/") || value.contains("\\") || value.contains(".."))
            throw new ProcessingException("FILE_IO_ERROR", "UPLOAD", "Unsafe filename");
        return Path.of(value).getFileName().toString();
    }
    static String extension(String filename) { return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT); }
    static String sha256(Path file) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); }
    private static void deleteQuietly(Path folder) { try { if (Files.exists(folder)) try (var files = Files.walk(folder)) { files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) { } }); } } catch (IOException ignored) { } }
}

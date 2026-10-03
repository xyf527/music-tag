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
    private final ResourceRepository resources;
    private final AudioTagHandlerRegistry handlers;

    public AudioFileService(StorageSettings storage, ResourceRepository resources, AudioTagHandlerRegistry handlers) {
        this.storage = storage; this.resources = resources; this.handlers = handlers;
    }

    public ResourceRecord upload(MultipartFile audio, MultipartFile lyrics, MultipartFile cover) {
        if (audio == null || audio.isEmpty()) throw new ProcessingException("FILE_IO_ERROR", "UPLOAD", "Audio file is empty");
        if (audio.getSize() > storage.maxUploadBytes()) throw new ProcessingException("INSUFFICIENT_STORAGE", "UPLOAD", "Audio file exceeds configured size limit");
        String filename = safeFilename(audio.getOriginalFilename());
        String extension = extension(filename);
        if (!extension.matches("mp3|flac|wav")) throw new ProcessingException("UNSUPPORTED_FORMAT", "UPLOAD", "Only MP3, FLAC and WAV are supported");
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

    private Path copyLyrics(MultipartFile file, Path folder) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > 2 * 1024 * 1024) throw new ProcessingException("INVALID_LRC", "UPLOAD", "Lyrics file is too large");
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (!text.isBlank() && !text.contains("[")) throw new ProcessingException("INVALID_LRC", "UPLOAD", "Lyrics must contain LRC metadata or timestamps");
        Path target = folder.resolve("lyrics.lrc"); Files.writeString(target, text, StandardCharsets.UTF_8); return target;
    }
    private Path copyCover(MultipartFile file, Path folder) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > 10 * 1024 * 1024) throw new ProcessingException("INVALID_COVER", "UPLOAD", "Cover file is too large");
        Path target = folder.resolve("cover"); Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        if (ImageIO.read(target.toFile()) == null) throw new ProcessingException("INVALID_COVER", "UPLOAD", "Cover is not a decodable image");
        return target;
    }
    private static void verifyMagic(Path file, String extension) throws IOException {
        byte[] bytes = Files.readAllBytes(file); String actual;
        if (extension.equals("flac") && bytes.length >= 4 && bytes[0] == 'f' && bytes[1] == 'L' && bytes[2] == 'a' && bytes[3] == 'C') actual = "flac";
        else if (extension.equals("wav") && bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E') actual = "wav";
        else if (extension.equals("mp3") && bytes.length >= 3 && ((bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') || (bytes[0] == (byte) 0xff && (bytes[1] & 0xe0) == 0xe0))) actual = "mp3";
        else actual = "unknown";
        if (!actual.equals(extension)) throw new ProcessingException("CORRUPT_AUDIO", "UPLOAD", "Extension does not match audio content");
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

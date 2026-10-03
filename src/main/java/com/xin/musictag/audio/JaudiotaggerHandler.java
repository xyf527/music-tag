package com.xin.musictag.audio;

import com.xin.musictag.domain.AudioResource;
import com.xin.musictag.domain.EditPlan;
import com.xin.musictag.domain.UpdateMode;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class JaudiotaggerHandler implements AudioTagHandler {
    private final String format;
    public JaudiotaggerHandler(String format) { this.format = format; }
    @Override public String format() { return format; }

    @Override public AudioResource read(Path file, String originalFilename) throws Exception {
        AudioFile audio = AudioFileIO.read(file.toFile());
        Tag tag = audio.getTag();
        Map<String,String> metadata = new LinkedHashMap<>();
        if (tag != null) {
            metadata.put("title", tag.getFirst(FieldKey.TITLE));
            metadata.put("artist", tag.getFirst(FieldKey.ARTIST));
            metadata.put("album", tag.getFirst(FieldKey.ALBUM));
            metadata.put("lyrics", tag.getFirst(FieldKey.LYRICS));
        }
        return new AudioResource(UUID.randomUUID().toString(), originalFilename, format,
                Files.size(file), sha256(file), file, metadata, Instant.now());
    }

    @Override public boolean supports(EditPlan plan) {
        return !format.equals("wav") || !modifies(plan);
    }
    @Override public void write(Path workingCopy, EditPlan plan, Path coverFile) throws Exception {
        if (format.equals("wav") && modifies(plan)) {
            throw new UnsupportedOperationException("WAV tag writing is unsupported by the verified adapter");
        }
        AudioFile audio = AudioFileIO.read(workingCopy.toFile());
        Tag tag = audio.getTagOrCreateAndSetDefault();
        set(tag, FieldKey.TITLE, plan.titleMode(), plan.title());
        set(tag, FieldKey.ARTIST, plan.artistMode(), plan.artist());
        set(tag, FieldKey.ALBUM, plan.albumMode(), plan.album());
        set(tag, FieldKey.LYRICS, plan.lyricsMode(), plan.lyrics());
        if (plan.coverMode() == UpdateMode.SET && coverFile != null) {
            Artwork artwork = Artwork.createArtworkFromFile(coverFile.toFile());
            artwork.setDescription("Phase 01 cover");
            tag.setField(artwork);
        } else if (plan.coverMode() == UpdateMode.REMOVE) {
            tag.deleteArtworkField();
        }
        audio.commit();
    }

    private static void set(Tag tag, FieldKey key, UpdateMode mode, String value) throws Exception {
        if (mode == UpdateMode.SET) tag.setField(key, value == null ? "" : value);
        if (mode == UpdateMode.REMOVE) tag.deleteField(key);
    }
    private static boolean modifies(EditPlan p) {
        return p.titleMode()!=UpdateMode.KEEP || p.artistMode()!=UpdateMode.KEEP || p.albumMode()!=UpdateMode.KEEP
                || p.lyricsMode()!=UpdateMode.KEEP || p.coverMode()!=UpdateMode.KEEP;
    }
    private static String sha256(Path file) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}

package com.example.musictag.tagging;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AudioTagProofOfConcept {
    public Map<String, String> readBasicText(Path audioFile) throws Exception {
        AudioFile file = AudioFileIO.read(audioFile.toFile());
        Tag tag = file.getTag();
        Map<String, String> values = new LinkedHashMap<>();
        if (tag != null) {
            values.put("title", tag.getFirst(FieldKey.TITLE));
            values.put("artist", tag.getFirst(FieldKey.ARTIST));
            values.put("album", tag.getFirst(FieldKey.ALBUM));
        }
        return values;
    }

    public void writeBasicText(Path workingCopy, String title, String artist) throws Exception {
        AudioFile file = AudioFileIO.read(workingCopy.toFile());
        Tag tag = file.getTagOrCreateAndSetDefault();
        tag.setField(FieldKey.TITLE, title);
        tag.setField(FieldKey.ARTIST, artist);
        file.commit();
    }

    public boolean remainsParseable(Path audioFile) {
        try {
            AudioFileIO.read(audioFile.toFile());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static Path copyTo(Path source, Path directory) throws IOException {
        Files.createDirectories(directory);
        return Files.copy(source, directory.resolve(source.getFileName()));
    }
}

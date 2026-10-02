package com.example.musictag.tagging;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AudioTagProofOfConcept {
    public Map<String, String> readBasicText(Path audioFile) throws Exception {
        Tag tag = AudioFileIO.read(audioFile.toFile()).getTag();
        Map<String, String> values = new LinkedHashMap<>();
        if (tag != null) {
            values.put("title", tag.getFirst(FieldKey.TITLE));
            values.put("artist", tag.getFirst(FieldKey.ARTIST));
            values.put("album", tag.getFirst(FieldKey.ALBUM));
            values.put("lyrics", tag.getFirst(FieldKey.LYRICS));
        }
        return values;
    }

    public void write(Path workingCopy, String title, String artist, String lyrics,
                      Path cover) throws Exception {
        AudioFile file = AudioFileIO.read(workingCopy.toFile());
        Tag tag = file.getTagOrCreateAndSetDefault();
        tag.setField(FieldKey.TITLE, title);
        tag.setField(FieldKey.ARTIST, artist);
        tag.setField(FieldKey.LYRICS, lyrics);
        if (cover != null) {
            Artwork artwork = Artwork.createArtworkFromFile(cover.toFile());
            artwork.setDescription("Phase 00 cover");
            tag.setField(artwork);
        }
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

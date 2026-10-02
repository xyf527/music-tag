package com.example.musictagger.tagging;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class JaudiotaggerAudioTagHandler implements AudioTagHandler {
    @Override
    public AudioMetadata read(Path audioFile) throws IOException {
        try {
            Tag tag = AudioFileIO.read(audioFile.toFile()).getTag();
            if (tag == null) {
                return new AudioMetadata(format(), null, null, null, null, false);
            }
            return new AudioMetadata(format(), first(tag, FieldKey.TITLE), first(tag, FieldKey.ARTIST),
                    first(tag, FieldKey.ALBUM), supportsLyrics() ? first(tag, FieldKey.LYRICS) : null,
                    !tag.getArtworkList().isEmpty());
        } catch (Exception e) {
            throw new IOException("Unable to read " + format() + " tags from " + audioFile, e);
        }
    }

    @Override
    public void write(Path workingCopy, TagWritePlan plan) throws IOException {
        if (!Files.isRegularFile(workingCopy)) {
            throw new IOException("Working copy does not exist: " + workingCopy);
        }
        try {
            AudioFile audioFile = AudioFileIO.read(workingCopy.toFile());
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            tag.setField(FieldKey.TITLE, plan.title());
            tag.setField(FieldKey.ARTIST, plan.artist());
            tag.setField(FieldKey.ALBUM, plan.album());
            if (supportsLyrics()) tag.setField(FieldKey.LYRICS, plan.lyrics());
            if (supportsArtwork() && plan.cover() != null) {
                tag.deleteArtworkField();
                tag.addField(ArtworkFactory.createArtworkFromFile(plan.cover().toFile()));
            }
            audioFile.commit();
        } catch (Exception e) {
            throw new IOException("Unable to write " + format() + " tags to " + workingCopy, e);
        }
    }

    private static String first(Tag tag, FieldKey key) {
        String value;
        try {
            value = tag.getFirst(key);
        } catch (UnsupportedOperationException unsupported) {
            return null;
        }
        return value == null || value.isBlank() ? null : value;
    }

    protected boolean supportsLyrics() { return true; }
    protected boolean supportsArtwork() { return true; }
}

package com.xin.musictag.tagging;

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
            AudioFile file = AudioFileIO.read(audioFile.toFile());
            validateContainer(file);
            Tag tag = file.getTag();
            if (tag == null) {
                return new AudioMetadata(format(), null, null, null, null, false,
                        file.getAudioHeader().getTrackLength() * 1000L,
                        file.getAudioHeader().getSampleRateAsNumber(), (int) file.getAudioHeader().getBitRateAsNumber());
            }
            return new AudioMetadata(format(), first(tag, FieldKey.TITLE), first(tag, FieldKey.ARTIST),
                    first(tag, FieldKey.ALBUM), supportsLyrics() ? first(tag, FieldKey.LYRICS) : null,
                    !tag.getArtworkList().isEmpty(), file.getAudioHeader().getTrackLength() * 1000L,
                    file.getAudioHeader().getSampleRateAsNumber(), (int) file.getAudioHeader().getBitRateAsNumber(),
                    !supportsArtwork()||tag.getFirstArtwork()==null?null:java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(tag.getFirstArtwork().getBinaryData())));
        } catch (com.xin.musictag.domain.ProcessingException e){throw e;
        } catch (Exception e) {
            throw new IOException("无法读取音频标签");
        }
    }

    @Override
    public void write(Path workingCopy, TagWritePlan plan) throws IOException {
        capabilities().require(plan);
        if (!Files.isRegularFile(workingCopy)) {
            throw new IOException("工作副本不可用");
        }
        try {
            AudioFile audioFile = AudioFileIO.read(workingCopy.toFile());
            validateContainer(audioFile);
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            apply(tag, FieldKey.TITLE, plan.title());
            apply(tag, FieldKey.ARTIST, plan.artist());
            apply(tag, FieldKey.ALBUM, plan.album());
            if (supportsLyrics()) apply(tag, FieldKey.LYRICS, plan.lyrics());
            if (supportsArtwork() && plan.artworkAction() != UpdateAction.KEEP) {
                if (plan.artworkAction() == UpdateAction.REMOVE) tag.deleteArtworkField();
                if (plan.artworkAction() == UpdateAction.SET) {
                    if (plan.cover() == null) throw new IOException("Cover file is required");
                    tag.deleteArtworkField();
                    tag.addField(ArtworkFactory.createArtworkFromFile(plan.cover().toFile()));
                }
            }
            audioFile.commit();
        } catch (com.xin.musictag.domain.ProcessingException e){throw e;
        } catch (Exception e) {
            throw new IOException("无法写入音频标签");
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

    private static void apply(Tag tag, FieldKey key, FieldChange change) throws Exception {
        if (change == null || change.action() == UpdateAction.KEEP) return;
        if (change.action() == UpdateAction.REMOVE) tag.deleteField(key);
        else tag.setField(key, change.value());
    }

    protected boolean supportsLyrics() { return true; }
    protected void validateContainer(AudioFile file)throws IOException{}
    protected boolean supportsArtwork() { return true; }
}

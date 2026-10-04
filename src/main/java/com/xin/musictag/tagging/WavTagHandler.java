package com.xin.musictag.tagging;

import org.jaudiotagger.audio.wav.WavOptions;
import org.jaudiotagger.tag.TagOptionSingleton;

import java.io.IOException;
import java.nio.file.Path;

public final class WavTagHandler extends JaudiotaggerAudioTagHandler {
    @Override public String format() { return "wav"; }
    @Override public boolean matchesContent(Path file)throws IOException{byte[] b=ContentSignatures.head(file);return ContentSignatures.at(b,0,"RIFF")&&ContentSignatures.at(b,8,"WAVE");}
    @Override public AudioMetadata read(Path audioFile) throws IOException {
        useInfoTagOnly();
        return super.read(audioFile);
    }
    @Override public void write(Path workingCopy, TagWritePlan plan) throws IOException {
        useInfoTagOnly();
        super.write(workingCopy, plan);
    }
    @Override public AudioCapabilities capabilities() {
        return new AudioCapabilities(true, false, false, "Jaudiotagger 3.0.1 WAV tag exposes RIFF INFO and embedded ID3 separately; FieldKey.LYRICS and COVER_ART are unsupported");
    }
    @Override protected boolean supportsLyrics() { return false; }
    @Override protected boolean supportsArtwork() { return false; }

    private static void useInfoTagOnly() {
        TagOptionSingleton.getInstance().setWavOptions(WavOptions.READ_INFO_ONLY);
    }
}

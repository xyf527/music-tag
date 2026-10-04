package com.xin.musictag.tagging;

import org.jaudiotagger.audio.wav.WavOptions;
import org.jaudiotagger.tag.TagOptionSingleton;

import java.io.IOException;
import java.nio.file.Path;

public final class WavTagHandler extends JaudiotaggerAudioTagHandler {
    @Override public String format() { return "wav"; }
    @Override public boolean matchesContent(Path file)throws IOException{byte[] b=ContentSignatures.head(file);return ContentSignatures.at(b,0,"RIFF")&&ContentSignatures.at(b,8,"WAVE");}
    @Override public AudioMetadata read(Path audioFile) throws IOException {
        useId3Tags();
        return super.read(audioFile);
    }
    @Override public void write(Path workingCopy, TagWritePlan plan) throws IOException {
        useId3Tags();
        super.write(workingCopy, plan);
    }
    @Override public AudioCapabilities capabilities() {
        return new AudioCapabilities(true, true, true, "WAV embedded ID3 lyrics/artwork; text tags synchronized with RIFF INFO. Player display depends on embedded ID3 support.");
    }
    private static void useId3Tags() {
        TagOptionSingleton.getInstance().setWavOptions(WavOptions.READ_ID3_ONLY_AND_SYNC);
        TagOptionSingleton.getInstance().setWavSaveOptions(org.jaudiotagger.audio.wav.WavSaveOptions.SAVE_BOTH_AND_SYNC);
    }
}

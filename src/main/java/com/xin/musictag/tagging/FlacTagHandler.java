package com.xin.musictag.tagging;

public final class FlacTagHandler extends JaudiotaggerAudioTagHandler {
    @Override public String format() { return "flac"; }
    @Override public boolean matchesContent(java.nio.file.Path file)throws java.io.IOException{return ContentSignatures.at(ContentSignatures.head(file),0,"fLaC");}
    @Override public AudioCapabilities capabilities() {
        return new AudioCapabilities(true, true, true, "Vorbis Comment/PICTURE; LRC is stored as text and player behavior varies");
    }
}

package com.example.musictagger.tagging;

public final class FlacTagHandler extends JaudiotaggerAudioTagHandler {
    @Override public String format() { return "flac"; }
    @Override public AudioCapabilities capabilities() {
        return new AudioCapabilities(true, true, true, "Vorbis Comment/PICTURE; LRC is stored as text and player behavior varies");
    }
}

package com.xin.musictag.tagging;

public final class Mp3TagHandler extends JaudiotaggerAudioTagHandler {
    @Override public String format() { return "mp3"; }
    @Override public AudioCapabilities capabilities() {
        return new AudioCapabilities(true, true, true, "ID3v2 text/APIC artwork/USLT lyrics; player display varies");
    }
}

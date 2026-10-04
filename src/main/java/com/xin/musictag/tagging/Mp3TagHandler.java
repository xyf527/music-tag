package com.xin.musictag.tagging;

public final class Mp3TagHandler extends JaudiotaggerAudioTagHandler {
    @Override public String format() { return "mp3"; }
    @Override public boolean matchesContent(java.nio.file.Path file)throws java.io.IOException{byte[] b=ContentSignatures.head(file);return ContentSignatures.at(b,0,"ID3")||b.length>=2&&b[0]==(byte)0xff&&(b[1]&0xe0)==0xe0;}
    @Override public AudioCapabilities capabilities() {
        return new AudioCapabilities(true, true, true, "ID3v2 text/APIC artwork/USLT lyrics; player display varies");
    }
}

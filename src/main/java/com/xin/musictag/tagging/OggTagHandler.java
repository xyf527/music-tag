package com.xin.musictag.tagging;

public final class OggTagHandler extends JaudiotaggerAudioTagHandler {
    public String format(){return "ogg";}
    public AudioCapabilities capabilities(){return new AudioCapabilities(true,true,true,"VERIFIED: OGG Vorbis comments/LYRICS/METADATA_BLOCK_PICTURE; Opus in OGG is UNSUPPORTED; player display pending.");}
    public boolean matchesContent(java.nio.file.Path file)throws java.io.IOException{return ContentSignatures.ogg(file,"\u0001vorbis");}
}

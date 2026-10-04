package com.xin.musictag.tagging;

/** Kept in the matrix for explicit rejection, never enabled for processing. */
public final class UnsupportedOpusHandler implements AudioTagHandler {
    public String format(){return "opus";}
    public AudioCapabilities capabilities(){return new AudioCapabilities(false,false,false,"UNSUPPORTED: jaudiotagger 3.0.1 cannot read/write Opus; generated Opus decoding alone does not prove tag support.",false,false);}
    public boolean matchesContent(java.nio.file.Path file)throws java.io.IOException{return ContentSignatures.ogg(file,"OpusHead");}
    private com.xin.musictag.domain.ProcessingException unsupported(){return new com.xin.musictag.domain.ProcessingException("UNSUPPORTED_FORMAT","READ","Opus 标签读取和写入未受当前库支持（UNSUPPORTED）");}
    public AudioMetadata read(java.nio.file.Path file){throw unsupported();}
    public void write(java.nio.file.Path file,TagWritePlan plan){throw unsupported();}
}

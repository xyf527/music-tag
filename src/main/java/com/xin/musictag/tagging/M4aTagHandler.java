package com.xin.musictag.tagging;

public final class M4aTagHandler extends JaudiotaggerAudioTagHandler {
    public String format(){return "m4a";}
    public AudioCapabilities capabilities(){return new AudioCapabilities(true,true,true,"VERIFIED: M4A AAC-LC; MP4 text/covr/lyrics. Other codecs are UNSUPPORTED; player display pending.");}
    public boolean matchesContent(java.nio.file.Path file)throws java.io.IOException{return ContentSignatures.at(ContentSignatures.head(file),4,"ftyp");}
    @Override protected void validateContainer(org.jaudiotagger.audio.AudioFile file)throws java.io.IOException {
        if(!(file.getAudioHeader() instanceof org.jaudiotagger.audio.mp4.Mp4AudioHeader header) || header.getProfile()!=org.jaudiotagger.audio.mp4.atom.Mp4EsdsBox.AudioProfile.LOW_COMPLEXITY)
            throw new com.xin.musictag.domain.ProcessingException("UNSUPPORTED_FORMAT","READ","M4A 仅验证 AAC-LC 音频，其他编码 UNSUPPORTED");
    }
}

package com.xin.musictag.tagging;

public record AudioCapabilities(
        boolean textTags,
        boolean artwork,
        boolean lyrics,
        String notes, boolean readable, boolean output) {
    public AudioCapabilities(boolean textTags,boolean artwork,boolean lyrics,String notes){this(textTags,artwork,lyrics,notes,true,true);}
    public void require(TagWritePlan plan) {
        if(!output || !textTags && java.util.stream.Stream.of(plan.title(),plan.artist(),plan.album()).anyMatch(c->c.action()!=UpdateAction.KEEP)) unsupported("标签写入");
        if(!lyrics && plan.lyrics().action()!=UpdateAction.KEEP) unsupported("歌词修改");
        if(!artwork && plan.artworkAction()!=UpdateAction.KEEP) unsupported("封面修改");
    }
    private void unsupported(String operation){throw new com.xin.musictag.domain.ProcessingException("UNSUPPORTED_FORMAT","PREVIEW",operation+"不受当前格式适配器支持（UNSUPPORTED）");}
}

package com.xin.musictag.tagging;

import java.util.Optional;

public record AudioMetadata(
        String format,
        String title,
        String artist,
        String album,
        String lyrics,
        boolean artworkPresent,
        long durationMs,
        int sampleRate,
        int bitrate, String artworkSha256) {
    public AudioMetadata(String format,String title,String artist,String album,String lyrics,boolean artworkPresent,long durationMs,int sampleRate,int bitrate){this(format,title,artist,album,lyrics,artworkPresent,durationMs,sampleRate,bitrate,null);}
    public Optional<String> titleValue() { return Optional.ofNullable(title); }
    public Optional<String> artistValue() { return Optional.ofNullable(artist); }
    public Optional<String> albumValue() { return Optional.ofNullable(album); }
    public Optional<String> lyricsValue() { return Optional.ofNullable(lyrics); }
}

package com.example.musictagger.tagging;

import java.util.Optional;

public record AudioMetadata(
        String format,
        String title,
        String artist,
        String album,
        String lyrics,
        boolean artworkPresent) {
    public Optional<String> titleValue() { return Optional.ofNullable(title); }
    public Optional<String> artistValue() { return Optional.ofNullable(artist); }
    public Optional<String> albumValue() { return Optional.ofNullable(album); }
    public Optional<String> lyricsValue() { return Optional.ofNullable(lyrics); }
}

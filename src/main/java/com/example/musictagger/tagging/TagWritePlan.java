package com.example.musictagger.tagging;

import java.nio.file.Path;

public record TagWritePlan(
        String title,
        String artist,
        String album,
        String lyrics,
        Path cover) {
    public static TagWritePlan demo(Path cover) {
        return new TagWritePlan("Phase 00 title", "Phase 00 artist", "Phase 00 album",
                "[00:00.00] Phase 00 lyrics", cover);
    }
}

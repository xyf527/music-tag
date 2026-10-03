package com.xin.musictag.domain;

public record EditPlan(String title, UpdateMode titleMode, String artist, UpdateMode artistMode,
                       String album, UpdateMode albumMode, String lyrics, UpdateMode lyricsMode,
                       byte[] cover, UpdateMode coverMode) {
    public static EditPlan keepAll() {
        return new EditPlan(null, UpdateMode.KEEP, null, UpdateMode.KEEP, null, UpdateMode.KEEP,
                null, UpdateMode.KEEP, null, UpdateMode.KEEP);
    }
}

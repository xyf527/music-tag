package com.example.musictagger.tagging;

public record AudioCapabilities(
        boolean textTags,
        boolean artwork,
        boolean lyrics,
        String notes) {
}

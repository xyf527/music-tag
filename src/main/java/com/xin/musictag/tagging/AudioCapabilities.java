package com.xin.musictag.tagging;

public record AudioCapabilities(
        boolean textTags,
        boolean artwork,
        boolean lyrics,
        String notes) {
}

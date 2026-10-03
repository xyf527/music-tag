package com.xin.musictag.tagging;

import java.nio.file.Path;

public record TagWritePlan(
        FieldChange title,
        FieldChange artist,
        FieldChange album,
        FieldChange lyrics,
        UpdateAction artworkAction,
        Path cover) {
    public static TagWritePlan demo(Path cover) {
        return new TagWritePlan(FieldChange.set("Phase 00 title"), FieldChange.set("Phase 00 artist"),
                FieldChange.set("Phase 00 album"), FieldChange.set("[00:00.00] Phase 00 lyrics"),
                UpdateAction.SET, cover);
    }
}

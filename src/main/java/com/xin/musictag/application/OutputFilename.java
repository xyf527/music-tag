package com.xin.musictag.application;

import com.xin.musictag.tagging.AudioMetadata;

/** Display filename only; never used as a storage path. */
public final class OutputFilename {
    private OutputFilename() { }
    public static String of(AudioMetadata metadata, String original, String format) {
        String fallback = original == null ? "未知歌曲" : original.replaceFirst("\\.[^.]+$", "");
        return clean(metadata.title(), fallback) + " - " + clean(metadata.artist(), "未知歌手") + "." + format;
    }
    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) value = fallback;
        value = value.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_")
                .replaceAll("[\\p{Cf}]", "").replaceAll("\\s+", " ").strip()
                .replaceAll("^[. ]+|[. ]+$", "");
        if (value.isBlank()) value = "未知";
        int count = value.codePointCount(0, value.length());
        return count > 60 ? value.substring(0, value.offsetByCodePoints(0, 60)) : value;
    }
}

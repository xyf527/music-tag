package com.xin.musictag.domain;

import java.util.List;

public record ProcessingPlan(int schemaVersion, long taskId, List<Entry> entries) {
    public record Entry(long itemId, long resourceId, String relativePath, boolean skip,
                        String lyricsText, Long coverItemId, String coverSha256) { }
}

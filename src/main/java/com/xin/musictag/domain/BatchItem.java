package com.xin.musictag.domain;

public record BatchItem(long id, long taskId, Long resourceId, String relativePath, String kind,
                        String status, String stage, String matchBasis, String lyricsName, String coverName,
                        Long outputVersionId, String errorCode, String userMessage, Long lyricsItemId,
                        Long coverItemId, String title, String artist, String candidatesJson, String diagnosticId) { }

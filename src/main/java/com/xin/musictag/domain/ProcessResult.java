package com.xin.musictag.domain;

public record ProcessResult(long taskId, long versionId, String status, String downloadUrl,
                            String reportUrl, String errorCode, String errorMessage) { }

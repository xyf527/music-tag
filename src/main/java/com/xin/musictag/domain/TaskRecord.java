package com.xin.musictag.domain;

import java.time.Instant;

public record TaskRecord(long id, long resourceId, String status, String stage, String errorCode,
                         String errorMessage, Long outputVersionId, String reportPath, Instant createdAt) { }

package com.xin.musictag.domain;

import java.time.Instant;

public record TaskRecord(String id, String status, String stage, String errorCode,
                         String resourceId, String versionId, String reportPath, Instant updatedAt) {}

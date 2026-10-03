package com.xin.musictag.domain;

import java.nio.file.Path;
import java.time.Instant;

public record ProcessingResult(String taskId, String status, String stage, String errorCode,
                               String versionId, Path output, String originalSha256,
                               String reportPath, Instant completedAt) {}

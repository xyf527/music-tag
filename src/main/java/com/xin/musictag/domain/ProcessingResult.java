package com.xin.musictag.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.file.Path;
import java.time.Instant;

public record ProcessingResult(String taskId, String status, String stage, String errorCode,
                               String versionId, @JsonIgnore Path output, String originalSha256,
                               @JsonIgnore String reportPath, Instant completedAt) {}

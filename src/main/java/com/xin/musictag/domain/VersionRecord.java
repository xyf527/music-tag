package com.xin.musictag.domain;

import java.nio.file.Path;
import java.time.Instant;

public record VersionRecord(long id, long sourceResourceId, Long parentVersionId, long taskId,
                            Path outputPath, String sha256, Instant createdAt) { }

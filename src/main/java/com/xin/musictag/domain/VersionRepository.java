package com.xin.musictag.domain;

import java.nio.file.Path;

public interface VersionRepository {
    VersionRecord create(long sourceResourceId, Long parentVersionId, long taskId,
                         Path outputPath, String sha256);
    VersionRecord require(long id);
    java.util.Optional<VersionRecord> latestForResource(long sourceResourceId);
}

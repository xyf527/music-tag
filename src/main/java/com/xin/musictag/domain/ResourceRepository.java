package com.xin.musictag.domain;

import java.nio.file.Path;
import java.util.Optional;

public interface ResourceRepository {
    ResourceRecord create(String originalFilename, String format, long byteSize, String sha256,
                          Path storagePath, Path lyricsPath, Path coverPath);
    Optional<ResourceRecord> find(long id);
}

package com.xin.musictag.domain;

import java.nio.file.Path;
import java.time.Instant;

public record ResourceRecord(long id, String originalFilename, String format, long byteSize,
                            String sha256, Path storagePath, Path lyricsPath, Path coverPath,
                            Instant createdAt) { }

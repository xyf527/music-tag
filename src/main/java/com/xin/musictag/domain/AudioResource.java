package com.xin.musictag.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

public record AudioResource(String id, String originalFilename, String format, long size, String sha256,
                            Path path, Map<String,String> metadata, Instant createdAt) {}

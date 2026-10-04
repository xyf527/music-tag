package com.xin.musictag.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("music.operations")
public record OperationsSettings(boolean minioEnabled, String endpoint, String accessKey, String secretKey,
                                 String bucket, String prefix, Duration retention, long minFreeBytes, double minFreePercent) {
    public OperationsSettings {
        if (prefix == null) prefix = "music";
        if (!prefix.matches("[a-zA-Z0-9_-]+(?:/[a-zA-Z0-9_-]+)*")) throw new IllegalArgumentException("Invalid backup prefix");
        if (retention == null) retention = Duration.ofDays(30);
        if (retention.isNegative() || retention.isZero() || minFreeBytes < 0 || minFreePercent < 0 || minFreePercent > 100) throw new IllegalArgumentException("Invalid storage policy");
    }
    public boolean configured() { return !minioEnabled || java.util.stream.Stream.of(endpoint, accessKey, secretKey, bucket).allMatch(v -> v != null && !v.isBlank()); }
}

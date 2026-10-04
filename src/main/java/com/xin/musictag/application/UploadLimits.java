package com.xin.musictag.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UploadLimits {
    private final long maxAudioBytes;
    private final long maxRequestBytes;
    private final int maxBatchFiles;
    private final long maxBatchBytes;

    public UploadLimits(
            @Value("${MUSIC_MAX_UPLOAD_BYTES:524288000}") long maxAudioBytes,
            @Value("${MUSIC_MAX_REQUEST_BYTES:576716800}") long maxRequestBytes,
            @Value("${MUSIC_MAX_BATCH_FILES:500}") int maxBatchFiles,
            @Value("${MUSIC_MAX_BATCH_BYTES:2147483648}") long maxBatchBytes) {
        if (maxAudioBytes <= 0) throw new IllegalArgumentException("MUSIC_MAX_UPLOAD_BYTES must be positive");
        if (maxRequestBytes <= maxAudioBytes) throw new IllegalArgumentException("MUSIC_MAX_REQUEST_BYTES must exceed MUSIC_MAX_UPLOAD_BYTES");
        if (maxBatchFiles <= 0 || maxBatchBytes <= 0) throw new IllegalArgumentException("批次限制必须大于零");
        this.maxAudioBytes = maxAudioBytes;
        this.maxRequestBytes = maxRequestBytes;
        this.maxBatchFiles = maxBatchFiles;
        this.maxBatchBytes = maxBatchBytes;
    }

    public long maxAudioBytes() { return maxAudioBytes; }
    public long maxRequestBytes() { return maxRequestBytes; }
    public int maxBatchFiles() { return maxBatchFiles; }
    public long maxBatchBytes() { return maxBatchBytes; }
    public String maxAudioMegabytes() {
        double megabytes = maxAudioBytes / 1024d / 1024d;
        return megabytes == Math.rint(megabytes) ? String.format("%.0f MB", megabytes) : String.format("%.1f MB", megabytes);
    }
}

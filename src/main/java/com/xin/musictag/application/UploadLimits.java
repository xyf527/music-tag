package com.xin.musictag.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UploadLimits {
    private final long maxAudioBytes;
    private final long maxRequestBytes;

    public UploadLimits(
            @Value("${MUSIC_MAX_UPLOAD_BYTES:524288000}") long maxAudioBytes,
            @Value("${MUSIC_MAX_REQUEST_BYTES:576716800}") long maxRequestBytes) {
        if (maxAudioBytes <= 0) throw new IllegalArgumentException("MUSIC_MAX_UPLOAD_BYTES must be positive");
        if (maxRequestBytes <= maxAudioBytes) throw new IllegalArgumentException("MUSIC_MAX_REQUEST_BYTES must exceed MUSIC_MAX_UPLOAD_BYTES");
        this.maxAudioBytes = maxAudioBytes;
        this.maxRequestBytes = maxRequestBytes;
    }

    public long maxAudioBytes() { return maxAudioBytes; }
    public long maxRequestBytes() { return maxRequestBytes; }
    public String maxAudioMegabytes() {
        double megabytes = maxAudioBytes / 1024d / 1024d;
        return megabytes == Math.rint(megabytes) ? String.format("%.0f MB", megabytes) : String.format("%.1f MB", megabytes);
    }
}

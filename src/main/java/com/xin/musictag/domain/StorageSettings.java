package com.xin.musictag.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("music.storage")
public record StorageSettings(Path root, long maxUploadBytes) {
    public StorageSettings {
        if (root == null) root = Path.of(".test-data/storage");
        if (maxUploadBytes <= 0) maxUploadBytes = 50L * 1024 * 1024;
    }
    public Path uploads() { return root.resolve("uploads"); }
    public Path working() { return root.resolve("working"); }
    public Path outputs() { return root.resolve("outputs"); }
    public Path reports() { return root.resolve("reports"); }
}

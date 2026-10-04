package com.xin.musictag.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.UUID;

/** Select ephemeral storage after application.yml has been loaded, before beans bind. */
public final class OptionalDatabaseEnvironment implements EnvironmentPostProcessor, Ordered {
    @Override public int getOrder() { return ConfigDataEnvironmentPostProcessor.ORDER + 1; }

    @Override public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        if (env.getProperty("music.database.enabled", Boolean.class, false)) return;
        var values = new HashMap<String, Object>();
        values.put("spring.datasource.url", "jdbc:h2:mem:music_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        values.put("spring.datasource.driver-class-name", "org.h2.Driver");
        values.put("spring.datasource.username", "sa");
        values.put("spring.datasource.password", "");
        values.put("spring.flyway.enabled", false);
        values.put("spring.flyway.url", values.get("spring.datasource.url"));
        values.put("spring.flyway.user", "sa");
        values.put("spring.flyway.password", "");
        values.put("music.operations.minio-enabled", false);
        values.put("music.database.enabled", false);
        values.put("spring.sql.init.mode", "always");
        values.put("spring.sql.init.schema-locations", String.join(",",
                "classpath:db/migration/V1__single_song.sql", "classpath:db/migration/V2__phase02_batch_import.sql",
                "classpath:db/migration/V3__batch_plan_and_attachment_state.sql", "classpath:db/migration/V4__batch_unique_bindings_and_execution.sql",
                "classpath:db/migration/V5__operations.sql"));
        try {
            var root = Files.createTempDirectory("music-tag-ephemeral-");
            values.put("music.storage.root", root.toString());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try (var paths = Files.walk(root)) {
                    paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (java.io.IOException ignored) { }
                    });
                } catch (java.io.IOException ignored) { }
            }, "music-tag-ephemeral-cleanup"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot initialize temporary audio storage");
        }
        env.getPropertySources().addFirst(new MapPropertySource("ephemeralMusicStorage", values));
    }
}

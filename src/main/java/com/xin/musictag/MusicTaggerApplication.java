package com.xin.musictag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.xin.musictag.domain.StorageSettings;

@SpringBootApplication
@EnableConfigurationProperties(StorageSettings.class)
public class MusicTaggerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusicTaggerApplication.class, args);
    }
}

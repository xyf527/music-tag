package com.xin.musictag.web;

import com.xin.musictag.application.UploadLimits;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class UploadConfiguration {
    @Bean
    MultipartConfigElement multipartConfig(UploadLimits limits) {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofBytes(limits.maxAudioBytes()));
        factory.setMaxRequestSize(DataSize.ofBytes(limits.maxRequestBytes()));
        return factory.createMultipartConfig();
    }
}

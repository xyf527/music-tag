package com.xin.musictag.infrastructure;

import com.xin.musictag.tagging.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AudioConfiguration {
    @Bean AudioTagHandlerRegistry audioTagHandlerRegistry() {
        return new AudioTagHandlerRegistry(List.of(new Mp3TagHandler(), new FlacTagHandler(), new WavTagHandler()));
    }
}

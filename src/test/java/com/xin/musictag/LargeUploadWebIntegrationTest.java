package com.xin.musictag;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "music.storage.root=.test-data/large-upload-${random.uuid}")
@ActiveProfiles("test")
class LargeUploadWebIntegrationTest {
    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @TempDir static Path samples;

    @BeforeAll
    static void createRealLargeMp3() throws Exception {
        List<String> command = new ArrayList<>(List.of("ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                "-f", "lavfi", "-i", "sine=frequency=330:duration=900", "-ar", "44100", "-ac", "1", "-b:a", "128k"));
        command.add(samples.resolve("over-12mb.mp3").toString());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), output);
        assertTrue(Files.size(samples.resolve("over-12mb.mp3")) > 12_200_000,
                "fixture must be a real MP3 larger than 12.2 MB");
    }

    @Test
    void uploadsMoreThan12Point2MbThroughRealHttpEndpoint() throws Exception {
        Path sample = samples.resolve("over-12mb.mp3");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("audio", new FileSystemResource(sample));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<String> response = rest.postForEntity("http://localhost:" + port + "/api/songs",
                new HttpEntity<>(body, headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("resourceId"));
    }
}

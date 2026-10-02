package com.example.musictag.tagging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AudioTagProofOfConceptTest {
    @Test
    void fixtureTestsAreSkippedWithoutExternalFixtureDirectory() {
        assumeTrue(System.getenv("MUSIC_TAG_FIXTURES_DIR") == null
                || System.getenv("MUSIC_TAG_FIXTURES_DIR").isBlank());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MUSIC_TAG_FIXTURES_DIR", matches = ".+")
    void copiesFixtureAndPreservesOriginalHash() throws Exception {
        Path source = Path.of(System.getenv("MUSIC_TAG_FIXTURES_DIR"), "sample.mp3");
        if (!Files.isRegularFile(source)) {
            return;
        }
        String before = sha256(source);
        Path work = Files.createTempDirectory("music-tag-poc-");
        Path copy = AudioTagProofOfConcept.copyTo(source, work);
        AudioTagProofOfConcept poc = new AudioTagProofOfConcept();
        poc.writeBasicText(copy, "Phase 00 title", "Phase 00 artist");
        assertTrue(poc.remainsParseable(copy));
        assertEquals(before, sha256(source));
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}

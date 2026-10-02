package com.example.musictag.tagging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AudioTagProofOfConceptTest {
    private static final List<String> FORMATS = List.of("mp3", "flac", "wav");

    @Test
    void fixtureAvailabilityIsExplicit() {
        String directory = System.getenv("MUSIC_TAG_FIXTURES_DIR");
        assumeTrue(directory != null && !directory.isBlank(),
                "MUSIC_TAG_FIXTURES_DIR is not set; fixture validation is NOT RUN");
        for (String format : FORMATS) {
            assertTrue(Files.isRegularFile(Path.of(directory, "sample." + format)),
                    "Missing required fixture: sample." + format);
        }
        assertTrue(Files.isRegularFile(Path.of(directory, "cover.png")), "Missing cover.png");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MUSIC_TAG_FIXTURES_DIR", matches = ".+")
    void copiesEachFixtureAndPreservesOriginalHash() throws Exception {
        Path fixtures = Path.of(System.getenv("MUSIC_TAG_FIXTURES_DIR"));
        Path work = Files.createTempDirectory("music-tag-poc-");
        AudioTagProofOfConcept poc = new AudioTagProofOfConcept();
        for (String format : FORMATS) {
            Path source = fixtures.resolve("sample." + format);
            assertTrue(Files.isRegularFile(source), "Missing required fixture: " + source);
            String before = sha256(source);
            Path copy = AudioTagProofOfConcept.copyTo(source, work);
            if (format.equals("wav")) {
                assertTrue(poc.remainsParseable(copy));
                try {
                    poc.write(copy, "Phase 00 " + format, "Phase 00 artist",
                            "[00:00.00]Phase 00 lyric", fixtures.resolve("cover.png"));
                } catch (UnsupportedOperationException expected) {
                    // Jaudiotagger 2.0.1 exposes WAV as a generic read-only tag.
                }
            } else {
                poc.write(copy, "Phase 00 " + format, "Phase 00 artist",
                        "[00:00.00]Phase 00 lyric", fixtures.resolve("cover.png"));
                assertTrue(poc.remainsParseable(copy));
                assertEquals("Phase 00 " + format, poc.readBasicText(copy).get("title"));
            }
            assertEquals(before, sha256(source));
        }
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}

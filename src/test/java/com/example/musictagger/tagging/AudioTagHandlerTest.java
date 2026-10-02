package com.example.musictagger.tagging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AudioTagHandlerTest {
    private static final Map<String, AudioTagHandler> HANDLERS = Map.of(
            "mp3", new Mp3TagHandler(), "flac", new FlacTagHandler(), "wav", new WavTagHandler());

    @Test
    void registryRejectsUnknownFormatAndResolvesCaseInsensitively() {
        var registry = new AudioTagHandlerRegistry(HANDLERS.values());
        assertSame(HANDLERS.get("wav"), registry.require("WAV"));
        assertThrows(IllegalArgumentException.class, () -> registry.require("m4a"));
    }

    @Test
    void writesAndReadsAllRequiredFormatsWithoutChangingOriginal(@TempDir Path work) throws Exception {
        Path samples = Path.of(System.getProperty("music.samples.dir", "target/phase-00-samples"));
        Path cover = createCover(work.resolve("cover.png"));
        for (String format : List.of("mp3", "flac", "wav")) {
            Path original = samples.resolve("original." + format);
            assertTrue(Files.isRegularFile(original), "missing sample: " + original
                    + "; run scripts/generate-samples.sh target/phase-00-samples");
            byte[] originalHash = sha256(Files.readAllBytes(original));
            byte[] decodedBefore = decodedPcm(original);
            Path copy = work.resolve("working." + format);
            Files.copy(original, copy);

            AudioTagHandler handler = HANDLERS.get(format);
            AudioMetadata before = handler.read(copy);
            handler.write(copy, TagWritePlan.demo(cover));
            AudioMetadata after = handler.read(copy);

            assertArrayEquals(originalHash, sha256(Files.readAllBytes(original)), format + " original changed");
            assertEquals("Phase 00 title", after.title(), format);
            assertEquals("Phase 00 artist", after.artist(), format);
            assertEquals("Phase 00 album", after.album(), format);
            if (handler.capabilities().lyrics()) {
                assertEquals("[00:00.00] Phase 00 lyrics", after.lyrics(), format);
            } else {
                assertNull(after.lyrics(), format + " lyrics must be reported unsupported");
            }
            assertEquals(handler.capabilities().artwork(), after.artworkPresent(), format + " artwork capability");
            assertNotNull(before);
            assertArrayEquals(decodedBefore, decodedPcm(copy), format + " decoded audio changed");
        }
    }

    private static Path createCover(Path path) throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) for (int y = 0; y < image.getHeight(); y++)
            image.setRGB(x, y, Color.ORANGE.getRGB());
        ImageIO.write(image, "png", path.toFile());
        return path;
    }

    private static byte[] sha256(byte[] input) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(input);
    }

    private static byte[] decodedPcm(Path file) throws Exception {
        Process process = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-i",
                file.toString(), "-map", "0:a:0", "-f", "s16le", "-acodec", "pcm_s16le", "-")
                .redirectErrorStream(true).start();
        byte[] output;
        try (InputStream input = process.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            input.transferTo(bytes);
            output = bytes.toByteArray();
        }
        assertEquals(0, process.waitFor(), "ffmpeg could not decode " + file);
        return output;
    }
}

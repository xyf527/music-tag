package com.example.musictagger.tagging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

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

@EnabledIfSystemProperty(named = "music.manual.output.dir", matches = ".+")
class ManualVerificationTest {
    private static final Map<String, AudioTagHandler> HANDLERS = Map.of(
            "mp3", new Mp3TagHandler(), "flac", new FlacTagHandler(), "wav", new WavTagHandler());

    @Test
    void writesInspectableArtifactsAndRecordsHashes() throws Exception {
        Path samples = Path.of(System.getProperty("music.samples.dir"));
        Path output = Path.of(System.getProperty("music.manual.output.dir"));
        Path processed = output.resolve("processed");
        Files.createDirectories(processed);
        Path cover = createCover(output.resolve("cover.png"));
        StringBuilder hashes = new StringBuilder();
        StringBuilder capabilities = new StringBuilder("format\tstatus\ttextTags\tartwork\tlyrics\tnotes\n");

        for (String format : List.of("mp3", "flac", "wav")) {
            Path original = samples.resolve("original." + format);
            assertTrue(Files.isRegularFile(original), "missing sample: " + original);
            Path artifact = processed.resolve("processed." + format);
            Files.copy(original, artifact, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            byte[] originalHash = sha256(Files.readAllBytes(original));
            byte[] decodedBefore = decodedPcm(original);
            AudioTagHandler handler = HANDLERS.get(format);
            handler.read(artifact);
            handler.write(artifact, TagWritePlan.demo(cover));
            AudioMetadata after = handler.read(artifact);

            assertEquals("Phase 00 title", after.title(), format);
            assertEquals("Phase 00 artist", after.artist(), format);
            assertEquals("Phase 00 album", after.album(), format);
            assertArrayEquals(originalHash, sha256(Files.readAllBytes(original)), format + " original changed");
            assertArrayEquals(decodedBefore, decodedPcm(artifact), format + " decoded audio changed");

            String originalHex = hex(originalHash);
            String processedHex = hex(sha256(Files.readAllBytes(artifact)));
            hashes.append(originalHex).append("  original/original.").append(format).append('\n');
            hashes.append(processedHex).append("  processed/processed.").append(format).append('\n');
            AudioCapabilities c = handler.capabilities();
            capabilities.append(format).append("\tVERIFIED\t").append(c.textTags()).append('\t')
                    .append(c.artwork() ? "VERIFIED" : "UNSUPPORTED").append('\t')
                    .append(c.lyrics() ? "VERIFIED" : "UNSUPPORTED").append('\t')
                    .append(c.notes()).append('\n');
        }
        Files.writeString(output.resolve("hashes.sha256"), hashes.toString());
        Files.writeString(output.resolve("capability-results.tsv"), capabilities.toString());
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

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    private static byte[] decodedPcm(Path file) throws Exception {
        Process process = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-i",
                file.toString(), "-map", "0:a:0", "-f", "s16le", "-acodec", "pcm_s16le", "-")
                .redirectErrorStream(true).start();
        try (InputStream input = process.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            input.transferTo(bytes);
            assertEquals(0, process.waitFor(), "ffmpeg could not decode " + file);
            return bytes.toByteArray();
        }
    }
}

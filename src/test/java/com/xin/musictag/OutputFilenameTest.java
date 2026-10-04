package com.xin.musictag;

import com.xin.musictag.application.OutputFilename;
import com.xin.musictag.tagging.AudioMetadata;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OutputFilenameTest {
    private AudioMetadata metadata(String title, String artist) {
        return new AudioMetadata("wav", title, artist, null, null, false, 0, 0, 0);
    }
    @Test void chineseAndFallbackNames() {
        assertEquals("歌曲 - 歌手.wav", OutputFilename.of(metadata("歌曲", "歌手"), "original.wav", "wav"));
        assertEquals("original - 未知歌手.wav", OutputFilename.of(metadata(null, " "), "original.wav", "wav"));
    }
    @Test void unsafeCharactersCannotProducePathsOrHeaderInjection() {
        String name = OutputFilename.of(metadata("../../歌\r\n曲", "a/b\\c"), "original.wav", "wav");
        assertFalse(name.contains("/")); assertFalse(name.contains("\\")); assertFalse(name.contains("\r"));
        assertFalse(name.contains("\n")); assertFalse(name.startsWith("."));
        assertTrue(name.endsWith(".wav"));
    }
}

package com.xin.musictag.tagging;

import java.io.IOException;
import java.nio.file.Path;

public interface AudioTagHandler {
    String format();
    AudioCapabilities capabilities();
    default boolean matchesContent(Path file) throws IOException { return false; }
    AudioMetadata read(Path audioFile) throws IOException;
    void write(Path workingCopy, TagWritePlan plan) throws IOException;
}

package com.example.musictagger.tagging;

import java.io.IOException;
import java.nio.file.Path;

public interface AudioTagHandler {
    String format();
    AudioCapabilities capabilities();
    AudioMetadata read(Path audioFile) throws IOException;
    void write(Path workingCopy, TagWritePlan plan) throws IOException;
}

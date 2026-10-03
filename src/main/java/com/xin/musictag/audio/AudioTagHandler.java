package com.xin.musictag.audio;

import com.xin.musictag.domain.AudioResource;
import com.xin.musictag.domain.EditPlan;

import java.nio.file.Path;

public interface AudioTagHandler {
    String format();
    AudioResource read(Path file, String originalFilename) throws Exception;
    void write(Path workingCopy, EditPlan plan, Path coverFile) throws Exception;
    boolean supports(EditPlan plan);
}

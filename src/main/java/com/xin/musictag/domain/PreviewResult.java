package com.xin.musictag.domain;

import com.xin.musictag.tagging.AudioCapabilities;
import com.xin.musictag.tagging.AudioMetadata;

import java.util.List;

public record PreviewResult(long resourceId, AudioMetadata current, TagEditPlan plan,
                           List<String> changes, List<String> warnings, AudioCapabilities capabilities) { }

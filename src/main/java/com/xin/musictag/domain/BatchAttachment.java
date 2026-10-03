package com.xin.musictag.domain;

import java.nio.file.Path;

/** Internal storage reference; never serialized into page or report responses. */
public record BatchAttachment(long itemId, Path path, String sha256, String lyricsText, String title, String artist) { }

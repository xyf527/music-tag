package com.xin.musictag.domain;

public record ResourceRef(String resourceId, String originalFilename, String format, long size,
                          String sha256, java.util.Map<String,String> metadata) {}

package com.xin.musictag.domain;
import java.time.Instant;
public record BatchTask(long id, String status, String planJson, Instant createdAt, Instant updatedAt) {}

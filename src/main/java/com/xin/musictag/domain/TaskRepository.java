package com.xin.musictag.domain;

import java.nio.file.Path;

public interface TaskRepository {
    TaskRecord create(long resourceId);
    void updateStatus(long taskId, String status, String stage, String errorCode, String errorMessage);
    void attachVersion(long taskId, long versionId);
    void attachReport(long taskId, Path reportPath);
    TaskRecord require(long taskId);
}

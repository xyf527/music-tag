package com.xin.musictag.web;

import com.xin.musictag.domain.TaskRecord;

public record TaskResponse(long id, long resourceId, String status, String stage,
                           String errorCode, String errorMessage, Long outputVersionId,
                           String downloadUrl, String reportUrl) {
    static TaskResponse from(TaskRecord task) {
        String download = task.outputVersionId() == null ? null : "/api/versions/" + task.outputVersionId() + "/download";
        String report = "/api/tasks/" + task.id() + "/report";
        return new TaskResponse(task.id(), task.resourceId(), task.status(), task.stage(), task.errorCode(),
                task.errorMessage(), task.outputVersionId(), download, report);
    }
}

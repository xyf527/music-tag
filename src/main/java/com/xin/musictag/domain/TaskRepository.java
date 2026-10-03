package com.xin.musictag.domain;

import java.util.Optional;

public interface TaskRepository {
    void save(TaskRecord task);
    Optional<TaskRecord> find(String id);
}

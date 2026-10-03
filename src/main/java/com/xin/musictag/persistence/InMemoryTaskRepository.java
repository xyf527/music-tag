package com.xin.musictag.persistence;

import com.xin.musictag.domain.TaskRecord;
import com.xin.musictag.domain.TaskRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public final class InMemoryTaskRepository implements TaskRepository {
    private final Map<String, TaskRecord> tasks = new ConcurrentHashMap<>();
    public void save(TaskRecord task) { tasks.put(task.id(), task); }
    public Optional<TaskRecord> find(String id) { return Optional.ofNullable(tasks.get(id)); }
}

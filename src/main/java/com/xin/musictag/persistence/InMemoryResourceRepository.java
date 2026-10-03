package com.xin.musictag.persistence;

import com.xin.musictag.domain.AudioResource;
import com.xin.musictag.domain.ResourceRepository;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;

@Profile("!mysql")
@Repository
public final class InMemoryResourceRepository implements ResourceRepository {
    private final Map<String, AudioResource> values = new ConcurrentHashMap<>();
    public void save(AudioResource resource) { values.put(resource.id(), resource); }
    public Optional<AudioResource> find(String id) { return Optional.ofNullable(values.get(id)); }
}

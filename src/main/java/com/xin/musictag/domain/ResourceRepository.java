package com.xin.musictag.domain;

import java.util.Optional;

public interface ResourceRepository {
    void save(AudioResource resource);
    Optional<AudioResource> find(String id);
}

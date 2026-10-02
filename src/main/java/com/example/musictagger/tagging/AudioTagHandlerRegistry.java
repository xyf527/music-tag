package com.example.musictagger.tagging;

import java.util.Locale;
import java.util.Map;

public final class AudioTagHandlerRegistry {
    private final Map<String, AudioTagHandler> handlers;

    public AudioTagHandlerRegistry(Iterable<AudioTagHandler> implementations) {
        var mutable = new java.util.HashMap<String, AudioTagHandler>();
        for (AudioTagHandler handler : implementations) {
            String key = handler.format().toLowerCase(Locale.ROOT);
            if (mutable.put(key, handler) != null) throw new IllegalArgumentException("Duplicate format: " + key);
        }
        handlers = Map.copyOf(mutable);
    }

    public AudioTagHandler require(String format) {
        AudioTagHandler result = handlers.get(format.toLowerCase(Locale.ROOT));
        if (result == null) throw new IllegalArgumentException("Unsupported audio format: " + format);
        return result;
    }
}

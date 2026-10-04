package com.xin.musictag.tagging;

import java.util.Locale;
import java.util.Map;

public final class AudioTagHandlerRegistry {
    private final Map<String, AudioTagHandler> handlers;

    public AudioTagHandlerRegistry(Iterable<AudioTagHandler> implementations) {
        var mutable = new java.util.HashMap<String, AudioTagHandler>();
        for (AudioTagHandler handler : implementations) {
            String key = handler.format().toLowerCase(Locale.ROOT);
            if(!key.matches("[a-z0-9]{1,16}"))throw new IllegalArgumentException("格式注册名称无效");
            if (mutable.put(key, handler) != null) throw new IllegalArgumentException("Duplicate format: " + key);
        }
        handlers = Map.copyOf(mutable);
    }

    public AudioTagHandler require(String format) {
        AudioTagHandler result = handlers.get(format.toLowerCase(Locale.ROOT));
        if (result == null) throw new com.xin.musictag.domain.ProcessingException("UNSUPPORTED_FORMAT","READ","未支持的音频格式（UNSUPPORTED）");
        return result;
    }
    public boolean supportsOutput(String extension){var handler=handlers.get(extension.toLowerCase(Locale.ROOT));return handler!=null&&handler.capabilities().output();}
    public Map<String,AudioCapabilities> matrix(){var matrix=new java.util.TreeMap<String,AudioCapabilities>();handlers.forEach((key,value)->matrix.put(key,value.capabilities()));return java.util.Collections.unmodifiableMap(matrix);}
    public java.util.List<String> outputFormats(){return matrix().entrySet().stream().filter(e->e.getValue().output()).map(e->e.getKey().toUpperCase(Locale.ROOT)).toList();}
}

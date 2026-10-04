package com.xin.musictag.application;

public interface BackupStore {
    void put(String key, byte[] content, String hash) throws Exception;
    default void putFile(String key, java.nio.file.Path file, String hash) throws Exception { put(key,java.nio.file.Files.readAllBytes(file),hash); }
    boolean ready();
}

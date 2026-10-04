package com.xin.musictag.application;

import com.xin.musictag.domain.*;
import org.springframework.stereotype.Component;
import java.nio.file.*;

@Component
public class DiskGuard {
    private final StorageSettings storage; private final OperationsSettings settings;
    public DiskGuard(StorageSettings storage, OperationsSettings settings) { this.storage = storage; this.settings = settings; }
    public static boolean sufficient(long usable, long total, long minimum, double percent) {
        return usable > minimum && (total > 0 && usable * 100.0 / total > percent);
    }
    public boolean available() {
        try { for(Path directory:java.util.List.of(storage.uploads(),storage.working(),storage.outputs(),storage.reports())) {
                Files.createDirectories(directory);var store=Files.getFileStore(directory);
                if(!sufficient(store.getUsableSpace(),store.getTotalSpace(),settings.minFreeBytes(),settings.minFreePercent()))return false;
            }
            return true;
        } catch (Exception e) { return false; }
    }
    public void requireUpload() { if (!available()) throw new ProcessingException("DISK_LOW", "UPLOAD", "磁盘空间不足，已暂停新上传；查询和下载仍可使用"); }
}

package com.xin.musictag.domain;

import java.util.List;
import java.util.Set;

public interface BatchRepository {
    BatchTask create();
    BatchTask require(long id);
    void lockDraft(long taskId);
    List<BatchItem> items(long taskId);
    BatchItem add(long taskId, Long resourceId, String path, String kind, String status, String basis,
                  String lyrics, String cover, String message);
    void attachment(BatchAttachment attachment);
    BatchAttachment attachment(long itemId);
    void metadata(long itemId, String title, String artist);
    void match(long itemId, Long lyrics, Long cover, String status, String basis, String candidates, String warning);
    void plan(long taskId, String json);
    boolean start(long taskId, Set<String> allowedStatuses);
    boolean claim(long itemId, Set<String> allowedStatuses);
    void taskStatus(long taskId, String status);
    void itemStatus(long id, String status, String stage, Long version, String code, String message);
    void recoverInterrupted();
}

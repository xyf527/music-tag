package com.xin.musictag.domain;
import java.util.List;
public interface BatchRepository {
 BatchTask create(); BatchTask require(long id); List<BatchItem> items(long taskId);
 BatchItem add(long taskId, Long resourceId, String path, String kind, String status, String basis, String lyrics, String cover, String message);
 void plan(long taskId, String json); void taskStatus(long taskId, String status); void itemStatus(long id, String status, String stage, Long version, String code, String message);
 void recoverInterrupted();
}

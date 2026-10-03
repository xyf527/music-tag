package com.xin.musictag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.musictag.application.*;
import com.xin.musictag.config.BatchExecutorConfiguration;
import com.xin.musictag.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BatchConcurrencyAndLimitsTest {
    @ParameterizedTest @ValueSource(ints={2,4,99})
    void itemWorkersRunInParallelWithinGlobalLimitAndIsolateFailure(int configured) throws Exception {
        int bound = Math.min(configured,4);
        var executor = new BatchExecutorConfiguration().batchExecutor(configured);
        BatchRepository repository = mock(BatchRepository.class);
        SingleSongService songs = mock(SingleSongService.class);
        ObjectMapper mapper = new ObjectMapper();
        List<ProcessingPlan.Entry> entries = new ArrayList<>();
        Map<Long,String> statuses = new ConcurrentHashMap<>();
        for (long id=1;id<=12;id++) { entries.add(new ProcessingPlan.Entry(id,id,"item-" + id + ".mp3",false,null,null,null)); statuses.put(id,"PENDING"); }
        when(repository.require(1)).thenReturn(new BatchTask(1,"CONFIRMED",mapper.writeValueAsString(new ProcessingPlan(1,1,entries)),Instant.now(),Instant.now()));
        when(repository.start(eq(1L),anySet())).thenReturn(true);
        when(repository.items(1)).thenAnswer(call -> statuses.entrySet().stream().map(e -> item(e.getKey(),e.getValue())).toList());
        when(repository.claim(anyLong(),anySet())).thenAnswer(call -> { statuses.put(call.getArgument(0),"RUNNING"); return true; });
        doAnswer(call -> { statuses.put(call.getArgument(0),call.getArgument(1)); return null; }).when(repository).itemStatus(anyLong(),anyString(),anyString(),nullable(Long.class),nullable(String.class),nullable(String.class));
        CountDownLatch firstWave = new CountDownLatch(bound), completed = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger(), peak = new AtomicInteger();
        when(songs.processBatch(anyLong(),any(),isNull(),anyLong())).thenAnswer(call -> {
            int count = active.incrementAndGet(); peak.accumulateAndGet(count,Math::max); firstWave.countDown();
            try {
                assertTrue(firstWave.await(5,TimeUnit.SECONDS),"items must actually run concurrently");
                Thread.sleep(20);
                long id = call.getArgument(0);
                return id == 12 ? new ProcessResult(id,0,"FAILED",null,null,"CORRUPT_AUDIO","failure") : new ProcessResult(id,id,"SUCCEEDED",null,null,null,null);
            } finally { active.decrementAndGet(); }
        });
        doAnswer(call -> { completed.countDown(); return null; }).when(repository).taskStatus(eq(1L),anyString());
        BatchImportService service = new BatchImportService(repository,mock(AudioFileService.class),songs,mock(VersionRepository.class),mapper,executor,configured,new UploadLimits(100,200,100,1000),new StorageSettings(Path.of(".test-data/unit"),100));
        try {
            assertEquals(bound,executor.getMaxPoolSize());
            service.execute(1,false); assertTrue(completed.await(10,TimeUnit.SECONDS));
            assertEquals(bound,peak.get(),"maximum concurrency must equal configured cap");
            assertEquals(11,statuses.values().stream().filter("SUCCESS"::equals).count());
            assertEquals("FAILED",statuses.get(12L));
            verify(repository).taskStatus(1,"COMPLETED_WITH_FAILURES");
        } finally { executor.shutdown(); }
    }

    @Test void batchLimitsRejectCountTotalBytesAndEmptyBeforeCreatingTask() {
        BatchRepository repository = mock(BatchRepository.class);
        BatchImportService service = new BatchImportService(repository,mock(AudioFileService.class),mock(SingleSongService.class),mock(VersionRepository.class),new ObjectMapper(),Runnable::run,2,new UploadLimits(100,200,2,10),new StorageSettings(Path.of(".test-data/unit"),100));
        var small = new MockMultipartFile("files","one.txt","text/plain",new byte[6]);
        assertTrue(assertThrows(IllegalArgumentException.class,() -> service.importFiles(List.of(small,small,small),List.of("a","b","c"))).getMessage().contains("文件数量"));
        assertTrue(assertThrows(IllegalArgumentException.class,() -> service.importFiles(List.of(small,small),List.of("a","b"))).getMessage().contains("总大小"));
        assertTrue(assertThrows(IllegalArgumentException.class,() -> service.importFiles(List.of(new MockMultipartFile("files","empty.txt","text/plain",new byte[0])),List.of("empty.txt"))).getMessage().contains("空文件"));
        verify(repository,never()).create();
        assertEquals(2,service.policy().get("maxFiles")); assertEquals(10L,service.policy().get("maxBytes"));
    }
    private static BatchItem item(long id,String status) {
        return new BatchItem(id,1,id,"item-" + id + ".mp3","AUDIO",status,"WRITE","UNBOUND",null,null,null,null,null,null,null,null,null,null,null);
    }
}

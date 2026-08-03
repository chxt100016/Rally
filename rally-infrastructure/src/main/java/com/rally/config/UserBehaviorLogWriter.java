package com.rally.config;

import com.rally.domain.behavior.gateway.UserBehaviorLogRepository;
import com.rally.domain.behavior.model.UserBehaviorLogData;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class UserBehaviorLogWriter {

    private final UserBehaviorLogRepository repository;
    private final ThreadPoolExecutor executor;
    private final AtomicLong droppedCount = new AtomicLong();

    public UserBehaviorLogWriter(
            UserBehaviorLogRepository repository,
            @Value("${behavior-log.queue-capacity:2000}") int queueCapacity) {
        this.repository = repository;
        int capacity = Math.max(1, queueCapacity);
        this.executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity),
                threadFactory(),
                (task, pool) -> reportDropped());
    }

    public void submit(UserBehaviorLogData data) {
        if (executor.isShutdown()) {
            reportDropped();
            return;
        }
        try {
            executor.execute(() -> saveQuietly(data));
        } catch (RuntimeException ex) {
            reportDropped();
        }
    }

    private void saveQuietly(UserBehaviorLogData data) {
        try {
            repository.save(data);
        } catch (RuntimeException ex) {
            log.warn("Failed to save user behavior log, requestId={}", data.getRequestId(), ex);
        }
    }

    private void reportDropped() {
        long count = droppedCount.incrementAndGet();
        if (count == 1 || count % 1000 == 0) {
            log.warn("User behavior logs dropped: count={}", count);
        }
    }

    private ThreadFactory threadFactory() {
        return task -> {
            Thread thread = new Thread(task, "behavior-log-writer");
            thread.setDaemon(true);
            return thread;
        };
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}

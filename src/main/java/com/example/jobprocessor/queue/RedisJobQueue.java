package com.example.jobprocessor.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisJobQueue {

    private static final String QUEUE_KEY = "job:queue";
    private static final String DLQ_KEY = "job:dlq";
    private static final String LOCK_PREFIX = "job:lock:";

    private final StringRedisTemplate redisTemplate;

    public void enqueue(UUID jobId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, jobId.toString());
        log.debug("Enqueued job {}", jobId);
    }

    public UUID dequeue() {
        String id = redisTemplate.opsForList().leftPop(QUEUE_KEY);
        if (id == null) {
            return null;
        }
        return UUID.fromString(id);
    }

    public void moveToDlq(UUID jobId) {
        redisTemplate.opsForList().rightPush(DLQ_KEY, jobId.toString());
        log.info("Moved job {} to DLQ", jobId);
    }

    public Long getQueueDepth() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return size != null ? size : 0L;
    }

    public Long getDlqDepth() {
        Long size = redisTemplate.opsForList().size(DLQ_KEY);
        return size != null ? size : 0L;
    }

    /**
     * Try to acquire a distributed lock for the job.
     * Returns true if lock acquired.
     */
    public boolean tryLock(UUID jobId, long ttlMs) {
        String key = LOCK_PREFIX + jobId;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMillis(ttlMs));
        return Boolean.TRUE.equals(acquired);
    }

    public void releaseLock(UUID jobId) {
        redisTemplate.delete(LOCK_PREFIX + jobId);
    }

    public void extendLock(UUID jobId, long ttlMs) {
        String key = LOCK_PREFIX + jobId;
        redisTemplate.expire(key, ttlMs, TimeUnit.MILLISECONDS);
    }
}

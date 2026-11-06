package com.example.jobprocessor.metrics;

import com.example.jobprocessor.queue.RedisJobQueue;
import com.example.jobprocessor.repository.JobRepository;
import com.example.jobprocessor.entity.JobStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class JobMetrics {

    private final MeterRegistry registry;
    private final RedisJobQueue jobQueue;
    private final JobRepository jobRepository;

    private Counter successCounter;
    private Counter failureCounter;
    private Counter retryCounter;
    private Counter dlqCounter;
    private Timer processingTimer;
    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        successCounter = Counter.builder("jobs_processed_total")
                .tag("status", "success")
                .description("Total successfully processed jobs")
                .register(registry);

        failureCounter = Counter.builder("jobs_processed_total")
                .tag("status", "failure")
                .description("Total failed job attempts")
                .register(registry);

        retryCounter = Counter.builder("jobs_retries_total")
                .description("Total retry attempts")
                .register(registry);

        dlqCounter = Counter.builder("jobs_dlq_total")
                .description("Jobs moved to Dead Letter Queue")
                .register(registry);

        processingTimer = Timer.builder("job_processing_duration_seconds")
                .description("Job processing latency")
                .register(registry);

        Gauge.builder("job_queue_depth", jobQueue, RedisJobQueue::getQueueDepth)
                .description("Current Redis job queue depth")
                .register(registry);

        Gauge.builder("job_dlq_depth", jobQueue, RedisJobQueue::getDlqDepth)
                .description("Current Dead Letter Queue depth")
                .register(registry);

        Gauge.builder("job_active_workers", activeWorkers, AtomicInteger::get)
                .description("Currently active workers")
                .register(registry);

        Gauge.builder("jobs_by_status", this, m -> jobRepository.countByStatus(JobStatus.PENDING))
                .tag("status", "PENDING")
                .register(registry);

        Gauge.builder("jobs_by_status", this, m -> jobRepository.countByStatus(JobStatus.QUEUED))
                .tag("status", "QUEUED")
                .register(registry);

        Gauge.builder("jobs_by_status", this, m -> jobRepository.countByStatus(JobStatus.PROCESSING))
                .tag("status", "PROCESSING")
                .register(registry);

        Gauge.builder("jobs_by_status", this, m -> jobRepository.countByStatus(JobStatus.COMPLETED))
                .tag("status", "COMPLETED")
                .register(registry);

        Gauge.builder("jobs_by_status", this, m -> jobRepository.countByStatus(JobStatus.DEAD_LETTER_QUEUE))
                .tag("status", "DEAD_LETTER_QUEUE")
                .register(registry);
    }

    public void recordSuccess(String type, long durationMs) {
        successCounter.increment();
        processingTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordFailure(long durationMs) {
        failureCounter.increment();
        processingTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordRetry() {
        retryCounter.increment();
    }

    public void recordDlq() {
        dlqCounter.increment();
    }

    public void incrementActiveWorkers() {
        activeWorkers.incrementAndGet();
    }

    public void decrementActiveWorkers() {
        activeWorkers.decrementAndGet();
    }
}

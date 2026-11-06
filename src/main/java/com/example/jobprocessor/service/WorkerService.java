package com.example.jobprocessor.service;

import com.example.jobprocessor.config.AppProperties;
import com.example.jobprocessor.entity.Job;
import com.example.jobprocessor.entity.JobStatus;
import com.example.jobprocessor.handler.JobHandler;
import com.example.jobprocessor.metrics.JobMetrics;
import com.example.jobprocessor.queue.RedisJobQueue;
import com.example.jobprocessor.repository.JobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerService {

    private final RedisJobQueue jobQueue;
    private final JobRepository jobRepository;
    private final JobHistoryService historyService;
    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final JobMetrics metrics;
    private final Executor jobWorkerExecutor;
    private final List<JobHandler> handlers;

    private Map<String, JobHandler> handlerMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        handlerMap = handlers.stream()
                .collect(Collectors.toMap(JobHandler::getType, Function.identity()));
        log.info("Registered job handlers: {}", handlerMap.keySet());
    }

    /**
     * Continuous polling of the Redis queue.
     * Multiple instances can safely poll because of distributed locking.
     */
    @Scheduled(fixedDelayString = "${app.job.poll-interval-ms:500}")
    public void pollAndDispatch() {
        UUID jobId = jobQueue.dequeue();
        if (jobId == null) {
            return;
        }

        // Try to acquire lock
        if (!jobQueue.tryLock(jobId, props.getJobLockTtlMs())) {
            log.debug("Could not acquire lock for job {}, re-enqueueing", jobId);
            jobQueue.enqueue(jobId);
            return;
        }

        jobWorkerExecutor.execute(() -> processJob(jobId));
    }

    /**
     * Also pick up jobs that are ready for retry.
     */
    @Scheduled(fixedDelay = 2000)
    public void processRetries() {
        List<Job> ready = jobRepository.findByStatusAndNextRetryAtBefore(
                JobStatus.RETRYING, Instant.now());

        for (Job job : ready) {
            if (jobQueue.tryLock(job.getId(), props.getJobLockTtlMs())) {
                jobWorkerExecutor.execute(() -> processJob(job.getId()));
            }
        }
    }

    private void processJob(UUID jobId) {
        long start = System.currentTimeMillis();
        try {
            Optional<Job> opt = jobRepository.findById(jobId);
            if (opt.isEmpty()) {
                log.warn("Job {} not found in DB", jobId);
                return;
            }

            Job job = opt.get();

            // Guard against already completed / DLQ jobs
            if (job.getStatus() == JobStatus.COMPLETED ||
                    job.getStatus() == JobStatus.DEAD_LETTER_QUEUE) {
                return;
            }

            markProcessing(job);

            JobHandler handler = handlerMap.get(job.getType());
            if (handler == null) {
                failPermanently(job, "No handler registered for type: " + job.getType());
                return;
            }

            Map<String, Object> payload = objectMapper.readValue(
                    job.getPayload(), new TypeReference<>() {});

            String result = handler.execute(payload);
            markCompleted(job, result);

            metrics.recordSuccess(job.getType(), System.currentTimeMillis() - start);
            log.info("Job {} completed successfully", jobId);

        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage());
            handleFailure(jobId, e.getMessage());
            metrics.recordFailure(System.currentTimeMillis() - start);
        } finally {
            jobQueue.releaseLock(jobId);
        }
    }

    @Transactional
    protected void markProcessing(Job job) {
        JobStatus from = job.getStatus();
        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
        historyService.record(job.getId(), from, JobStatus.PROCESSING, "Worker started processing");
        metrics.incrementActiveWorkers();
    }

    @Transactional
    protected void markCompleted(Job job, String result) {
        job.setStatus(JobStatus.COMPLETED);
        job.setResult(result);
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(null);
        jobRepository.save(job);
        historyService.record(job.getId(), JobStatus.PROCESSING, JobStatus.COMPLETED, "Job completed");
        metrics.decrementActiveWorkers();
    }

    private void handleFailure(UUID jobId, String error) {
        jobRepository.findById(jobId).ifPresent(job -> {
            int nextRetry = job.getRetryCount() + 1;

            if (nextRetry > job.getMaxRetries()) {
                failPermanently(job, error);
            } else {
                scheduleRetry(job, nextRetry, error);
            }
            metrics.decrementActiveWorkers();
        });
    }

    @Transactional
    protected void scheduleRetry(Job job, int nextRetry, String error) {
        long delay = (long) (props.getRetryBaseDelayMs() * Math.pow(2, nextRetry - 1));
        Instant nextAt = Instant.now().plusMillis(delay);

        job.setStatus(JobStatus.RETRYING);
        job.setRetryCount(nextRetry);
        job.setErrorMessage(error);
        job.setNextRetryAt(nextAt);
        jobRepository.save(job);

        historyService.record(job.getId(), JobStatus.PROCESSING, JobStatus.RETRYING,
                "Retry #" + nextRetry + " scheduled in " + delay + "ms. Error: " + error);

        metrics.recordRetry();
        log.info("Job {} scheduled for retry #{} at {}", job.getId(), nextRetry, nextAt);
    }

    @Transactional
    protected void failPermanently(Job job, String error) {
        job.setStatus(JobStatus.DEAD_LETTER_QUEUE);
        job.setErrorMessage(error);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);

        historyService.record(job.getId(), JobStatus.PROCESSING, JobStatus.DEAD_LETTER_QUEUE,
                "Moved to DLQ after exhausting retries. Last error: " + error);

        jobQueue.moveToDlq(job.getId());
        metrics.recordDlq();
        log.warn("Job {} moved to DLQ", job.getId());
    }
}

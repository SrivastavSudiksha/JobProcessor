package com.example.jobprocessor.service;

import com.example.jobprocessor.config.AppProperties;
import com.example.jobprocessor.dto.JobRequest;
import com.example.jobprocessor.dto.JobResponse;
import com.example.jobprocessor.dto.JobSubmitResponse;
import com.example.jobprocessor.entity.Job;
import com.example.jobprocessor.entity.JobStatus;
import com.example.jobprocessor.queue.RedisJobQueue;
import com.example.jobprocessor.repository.JobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final JobHistoryService historyService;
    private final RedisJobQueue jobQueue;
    private final AppProperties props;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobSubmitResponse submit(JobRequest request) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(request.getPayload());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload");
        }

        Job job = Job.builder()
                .type(request.getType())
                .payload(payloadJson)
                .status(JobStatus.PENDING)
                .retryCount(0)
                .maxRetries(props.getMaxRetries())
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .build();

        job = jobRepository.save(job);
        historyService.record(job.getId(), null, JobStatus.PENDING, "Job created");

        // Move to queue
        job.setStatus(JobStatus.QUEUED);
        jobRepository.save(job);
        historyService.record(job.getId(), JobStatus.PENDING, JobStatus.QUEUED, "Pushed to queue");

        jobQueue.enqueue(job.getId());

        log.info("Submitted job {} of type {}", job.getId(), job.getType());

        return JobSubmitResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .build();
    }

    public JobResponse getJob(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        return toResponse(job);
    }

    public Page<JobResponse> listJobs(JobStatus status, Pageable pageable) {
        Page<Job> page = status != null
                ? jobRepository.findByStatus(status, pageable)
                : jobRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    public List<JobResponse> listDlqJobs() {
        return jobRepository.findAllByStatusOrderByCreatedAtAsc(JobStatus.DEAD_LETTER_QUEUE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public JobResponse requeueFromDlq(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (job.getStatus() != JobStatus.DEAD_LETTER_QUEUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job is not in DLQ");
        }

        JobStatus previous = job.getStatus();
        job.setStatus(JobStatus.QUEUED);
        job.setRetryCount(0);
        job.setErrorMessage(null);
        job.setNextRetryAt(null);
        jobRepository.save(job);

        historyService.record(jobId, previous, JobStatus.QUEUED, "Manually requeued from DLQ");
        jobQueue.enqueue(jobId);

        log.info("Requeued job {} from DLQ", jobId);
        return toResponse(job);
    }

    private JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .jobId(job.getId())
                .type(job.getType())
                .status(job.getStatus())
                .retryCount(job.getRetryCount())
                .maxRetries(job.getMaxRetries())
                .result(job.getResult())
                .errorMessage(job.getErrorMessage())
                .priority(job.getPriority())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}

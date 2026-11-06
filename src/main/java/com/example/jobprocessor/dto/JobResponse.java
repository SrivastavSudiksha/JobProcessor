package com.example.jobprocessor.dto;

import com.example.jobprocessor.entity.JobStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class JobResponse {

    private UUID jobId;
    private String type;
    private JobStatus status;
    private int retryCount;
    private int maxRetries;
    private String result;
    private String errorMessage;
    private String priority;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant updatedAt;
}

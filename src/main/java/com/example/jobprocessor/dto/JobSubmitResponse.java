package com.example.jobprocessor.dto;

import com.example.jobprocessor.entity.JobStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class JobSubmitResponse {
    private UUID jobId;
    private JobStatus status;
    private Instant createdAt;
}

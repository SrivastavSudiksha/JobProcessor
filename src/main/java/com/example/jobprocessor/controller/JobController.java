package com.example.jobprocessor.controller;

import com.example.jobprocessor.dto.JobRequest;
import com.example.jobprocessor.dto.JobResponse;
import com.example.jobprocessor.dto.JobSubmitResponse;
import com.example.jobprocessor.entity.JobStatus;
import com.example.jobprocessor.service.JobHistoryService;
import com.example.jobprocessor.service.JobService;
import com.example.jobprocessor.entity.JobHistory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobHistoryService historyService;

    @PostMapping
    public ResponseEntity<JobSubmitResponse> submit(@Valid @RequestBody JobRequest request) {
        JobSubmitResponse response = jobService.submit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{jobId}")
    public JobResponse getJob(@PathVariable UUID jobId) {
        return jobService.getJob(jobId);
    }

    @GetMapping
    public Page<JobResponse> listJobs(
            @RequestParam(required = false) JobStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return jobService.listJobs(status, pageable);
    }

    @GetMapping("/{jobId}/history")
    public List<JobHistory> getHistory(@PathVariable UUID jobId) {
        return historyService.getHistory(jobId);
    }
}

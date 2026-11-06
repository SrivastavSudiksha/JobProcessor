package com.example.jobprocessor.controller;

import com.example.jobprocessor.dto.JobResponse;
import com.example.jobprocessor.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
public class DlqController {

    private final JobService jobService;

    @GetMapping
    public List<JobResponse> listDlqJobs() {
        return jobService.listDlqJobs();
    }

    @PostMapping("/{jobId}/requeue")
    public JobResponse requeue(@PathVariable UUID jobId) {
        return jobService.requeueFromDlq(jobId);
    }
}

package com.example.jobprocessor.service;

import com.example.jobprocessor.entity.JobHistory;
import com.example.jobprocessor.entity.JobStatus;
import com.example.jobprocessor.repository.JobHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobHistoryService {

    private final JobHistoryRepository historyRepository;

    @Transactional
    public void record(UUID jobId, JobStatus from, JobStatus to, String message) {
        JobHistory history = JobHistory.builder()
                .jobId(jobId)
                .fromStatus(from)
                .toStatus(to)
                .message(message)
                .build();
        historyRepository.save(history);
    }

    public List<JobHistory> getHistory(UUID jobId) {
        return historyRepository.findByJobIdOrderByCreatedAtAsc(jobId);
    }
}

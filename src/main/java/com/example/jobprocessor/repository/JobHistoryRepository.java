package com.example.jobprocessor.repository;

import com.example.jobprocessor.entity.JobHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobHistoryRepository extends JpaRepository<JobHistory, UUID> {

    List<JobHistory> findByJobIdOrderByCreatedAtAsc(UUID jobId);
}

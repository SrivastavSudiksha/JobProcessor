package com.example.jobprocessor.repository;

import com.example.jobprocessor.entity.Job;
import com.example.jobprocessor.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    List<Job> findByStatusAndNextRetryAtBefore(JobStatus status, Instant time);

    long countByStatus(JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.status = :status ORDER BY j.createdAt ASC")
    List<Job> findAllByStatusOrderByCreatedAtAsc(@Param("status") JobStatus status);
}

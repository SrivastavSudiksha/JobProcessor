package com.example.jobprocessor.entity;

public enum JobStatus {
    PENDING,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    RETRYING,
    DEAD_LETTER_QUEUE
}

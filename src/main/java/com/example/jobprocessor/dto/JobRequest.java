package com.example.jobprocessor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class JobRequest {

    @NotBlank(message = "Job type is required")
    private String type;

    @NotNull(message = "Payload is required")
    private Map<String, Object> payload;

    private String priority = "NORMAL"; // HIGH, NORMAL, LOW
}

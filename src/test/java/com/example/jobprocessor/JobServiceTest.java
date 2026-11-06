package com.example.jobprocessor;

import com.example.jobprocessor.dto.JobRequest;
import com.example.jobprocessor.dto.JobSubmitResponse;
import com.example.jobprocessor.entity.JobStatus;
import com.example.jobprocessor.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JobServiceTest {

    @Autowired
    private JobService jobService;

    @Test
    void submitJob_shouldReturnQueuedStatus() {
        JobRequest request = new JobRequest();
        request.setType("resize_image");
        request.setPayload(Map.of(
                "url", "https://example.com/img.jpg",
                "width", 800,
                "height", 600
        ));
        request.setPriority("NORMAL");

        JobSubmitResponse response = jobService.submit(request);

        assertThat(response.getJobId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void getJob_shouldReturnDetails() {
        JobRequest request = new JobRequest();
        request.setType("send_email");
        request.setPayload(Map.of(
                "to", "test@example.com",
                "subject", "Hello",
                "body", "World"
        ));

        JobSubmitResponse submitted = jobService.submit(request);
        var job = jobService.getJob(submitted.getJobId());

        assertThat(job.getJobId()).isEqualTo(submitted.getJobId());
        assertThat(job.getType()).isEqualTo("send_email");
        assertThat(job.getStatus()).isIn(JobStatus.QUEUED, JobStatus.PROCESSING, JobStatus.COMPLETED);
    }
}

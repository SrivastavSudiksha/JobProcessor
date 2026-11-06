package com.example.jobprocessor.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class SendEmailHandler implements JobHandler {

    @Override
    public String getType() {
        return "send_email";
    }

    @Override
    public String execute(Map<String, Object> payload) throws Exception {
        String to = (String) payload.get("to");
        String subject = (String) payload.get("subject");
        String body = (String) payload.get("body");

        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("to is required");
        }

        log.info("Sending email to {} with subject '{}'", to, subject);

        Thread.sleep(400 + (long) (Math.random() * 400));

        if (Math.random() < 0.10) {
            throw new RuntimeException("SMTP temporary failure");
        }

        return String.format("{\"messageId\":\"msg-%d\",\"status\":\"sent\"}", System.currentTimeMillis());
    }
}

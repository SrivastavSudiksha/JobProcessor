package com.example.jobprocessor.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class GenerateReportHandler implements JobHandler {

    @Override
    public String getType() {
        return "generate_report";
    }

    @Override
    public String execute(Map<String, Object> payload) throws Exception {
        String reportType = (String) payload.get("reportType");
        if (reportType == null) {
            reportType = "default";
        }

        log.info("Generating report of type {}", reportType);

        // Simulate heavier work
        Thread.sleep(1200 + (long) (Math.random() * 1000));

        if (Math.random() < 0.12) {
            throw new RuntimeException("Report generation engine temporary error");
        }

        return String.format("{\"reportId\":\"rpt-%d\",\"type\":\"%s\",\"downloadUrl\":\"https://reports.example.com/%d.pdf\"}",
                System.currentTimeMillis(), reportType, System.currentTimeMillis());
    }
}

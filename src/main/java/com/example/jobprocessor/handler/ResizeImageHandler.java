package com.example.jobprocessor.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Example handler – simulates image resize work.
 * In a real system this would download the image and process it.
 */
@Component
@Slf4j
public class ResizeImageHandler implements JobHandler {

    @Override
    public String getType() {
        return "resize_image";
    }

    @Override
    public String execute(Map<String, Object> payload) throws Exception {
        String url = (String) payload.get("url");
        Object width = payload.get("width");
        Object height = payload.get("height");

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url is required");
        }

        log.info("Resizing image from {} to {}x{}", url, width, height);

        // Simulate work
        Thread.sleep(800 + (long) (Math.random() * 700));

        // Simulate occasional transient failure (~15%)
        if (Math.random() < 0.15) {
            throw new RuntimeException("Transient failure while downloading image");
        }

        return String.format("{\"resizedUrl\":\"https://cdn.example.com/resized/%s_%sx%s.jpg\",\"width\":%s,\"height\":%s}",
                System.currentTimeMillis(), width, height, width, height);
    }
}

package com.example.jobprocessor.handler;

import java.util.Map;

/**
 * Contract for concrete job type handlers.
 * Implement this interface and annotate with @Component to register a new job type.
 */
public interface JobHandler {

    /**
     * The job type this handler supports (must match JobRequest.type).
     */
    String getType();

    /**
     * Execute the job. Return a result string (or JSON) on success.
     * Throw an exception on failure so the worker can handle retries.
     */
    String execute(Map<String, Object> payload) throws Exception;
}

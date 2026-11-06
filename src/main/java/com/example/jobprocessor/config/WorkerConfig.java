package com.example.jobprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class WorkerConfig {

    @Bean(name = "jobWorkerExecutor")
    public Executor jobWorkerExecutor(AppProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getWorkerPoolSize());
        executor.setMaxPoolSize(props.getWorkerPoolSize());
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("job-worker-");
        executor.initialize();
        return executor;
    }
}

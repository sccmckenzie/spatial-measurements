package com.example.scanner;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class Runner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(Runner.class);
    private final ScanOrchestrationService scanOrchestrationService;
    private final static int POOL_SIZE = 5;
    private final static int NUMBER_OF_TASKS = 100;
    ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);


    @Override
    public void run(String... args) throws Exception {
        for (int i = 0; i < NUMBER_OF_TASKS; i++) {
            // int taskNumber = i;
            executor.submit(() -> {
                // String taskName = "task-" + taskNumber;
                // String threadName = Thread.currentThread().getName();
                // logger.info("{} executes {}", threadName, taskName);
                scanOrchestrationService.executeScan();
            });
        }
        
        // Add proper shutdown
        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        logger.info("All tasks completed: {}", finished);
    }
}

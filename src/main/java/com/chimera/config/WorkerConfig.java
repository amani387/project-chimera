package com.chimera.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Worker thread execution.
 */
@Configuration
public class WorkerConfig {

  @Bean
  public ExecutorService workerExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}

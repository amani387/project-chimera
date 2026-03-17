package com.chimera.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Planner background execution.
 */
@Configuration
public class PlannerConfig {

  @Bean
  public ExecutorService plannerExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}

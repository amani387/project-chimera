package com.chimera.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Judge thread execution.
 */
@Configuration
public class JudgeConfig {

  @Bean
  public ExecutorService judgeExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}

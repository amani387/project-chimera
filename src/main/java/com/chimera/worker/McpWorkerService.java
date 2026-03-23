package com.chimera.worker;

import com.chimera.mcp.WeaviateMcpClient;
import com.chimera.model.Task;
import com.chimera.model.WorkerResult;
import com.chimera.worker.WorkerService;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Worker service implementation that supports MCP-backed memory tasks.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "chimera.worker.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "chimera.worker.mcp.enabled", havingValue = "true")
public class McpWorkerService extends WorkerService {

  public McpWorkerService(
      @Qualifier("taskRedisTemplate") RedisTemplate<String, Task> taskRedisTemplate,
      @Qualifier("resultRedisTemplate") RedisTemplate<String, WorkerResult> resultRedisTemplate,
      ExecutorService workerExecutor,
      WeaviateMcpClient memoryClient) {
    super(taskRedisTemplate, resultRedisTemplate, workerExecutor);
    // memoryClient is injected into the parent class via field injection
  }
}

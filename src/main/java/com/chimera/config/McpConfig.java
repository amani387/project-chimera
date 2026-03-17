package com.chimera.config;

import com.chimera.mcp.MockWeaviateMcpClient;
import com.chimera.mcp.WeaviateMcpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MCP-related clients.
 */
@Slf4j
@Configuration
public class McpConfig {

  @Bean
  public WeaviateMcpClient weaviateMcpClient(
      @Value("${chimera.weaviate.mock:true}") boolean useMock,
      @Value("${chimera.weaviate.base-url:http://localhost:8081}") String baseUrl) {
    if (useMock) {
      log.info("Using mock Weaviate MCP client");
      return new MockWeaviateMcpClient();
    }
    log.info("Using HTTP Weaviate MCP client (baseUrl={})", baseUrl);
    // Placeholder: real implementation should call an MCP server.
    return new MockWeaviateMcpClient();
  }
}

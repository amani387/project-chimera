package com.chimera.config;

import com.chimera.mcp.HttpWeaviateMcpClient;
import com.chimera.mcp.MockWeaviateMcpClient;
import com.chimera.mcp.WeaviateMcpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Configuration for MCP-related clients.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(WeaviateProperties.class)
public class McpConfig {

  @Bean
  @ConditionalOnProperty(name = "chimera.weaviate.mock", havingValue = "true", matchIfMissing = true)
  public WeaviateMcpClient mockWeaviateMcpClient() {
    log.info("Using mock Weaviate MCP client");
    return new MockWeaviateMcpClient();
  }

  @Bean
  @ConditionalOnProperty(name = "chimera.weaviate.mock", havingValue = "false")
  public WeaviateMcpClient httpWeaviateMcpClient(WeaviateProperties props) {
    log.info("Using HTTP Weaviate MCP client (url={})", props.getUrl());

    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(props.getConnectTimeout());
    requestFactory.setReadTimeout(props.getReadTimeout());

    var restTemplate = new RestTemplate(requestFactory);
    restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(props.getUrl()));

    return new HttpWeaviateMcpClient(restTemplate, props);
  }
}

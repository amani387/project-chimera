package com.chimera.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for connecting to a Weaviate instance.
 */
@ConfigurationProperties(prefix = "chimera.weaviate")
public class WeaviateProperties {

  /**
   * Base URL for the Weaviate instance (including protocol).
   */
  private String url = "http://localhost:8080";

  /**
   * The Weaviate class name to use for storing memory entries.
   */
  private String className = "Memory";

  /**
   * Whether to use the in-memory mock client instead of a real Weaviate instance.
   */
  private boolean mock = true;

  /**
   * Connection timeout in milliseconds.
   */
  private int connectTimeout = 5000;

  /**
   * Read timeout in milliseconds.
   */
  private int readTimeout = 10000;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public boolean isMock() {
    return mock;
  }

  public void setMock(boolean mock) {
    this.mock = mock;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }
}

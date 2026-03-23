package com.chimera.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chimera.image")
public class ImageProperties {

  private Provider provider = Provider.MOCK;
  private int width = 1024;
  private int height = 1024;
  private boolean mock = true;

  public enum Provider {
    POLLINATIONS,
    MOCK
  }

  // Getters/Setters
  public Provider getProvider() { return provider; }
  public void setProvider(Provider provider) { this.provider = provider; }

  public int getWidth() { return width; }
  public void setWidth(int width) { this.width = width; }

  public int getHeight() { return height; }
  public void setHeight(int height) { this.height = height; }

  public boolean isMock() { return mock; }
  public void setMock(boolean mock) { this.mock = mock; }
}

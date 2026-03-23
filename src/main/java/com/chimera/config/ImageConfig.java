package com.chimera.config;

import com.chimera.service.ImageGenerationService;
import com.chimera.service.MockImageService;
import com.chimera.service.PollinationsImageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ImageProperties.class)
public class ImageConfig {

  @Bean
  @ConditionalOnProperty(name = "chimera.image.provider", havingValue = "mock", matchIfMissing = true)
  public ImageGenerationService mockImageService() {
    return new MockImageService();
  }

@Bean
  @ConditionalOnProperty(name = "chimera.image.provider", havingValue = "pollinations")
  public ImageGenerationService pollinationsImageService(ImageProperties props) {
    return new PollinationsImageService(props);
  }
}



package com.chimera.config;

import com.chimera.service.GroqLLMService;
import com.chimera.service.LLMService;
import com.chimera.service.MockLLMService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LLMProperties.class)
public class LLMConfig {

    @Bean
    @ConditionalOnProperty(name = "chimera.llm.provider", havingValue = "mock", matchIfMissing = true)
    public LLMService mockLLMService() {
        return new MockLLMService();
    }

    @Bean
    @ConditionalOnProperty(name = "chimera.llm.provider", havingValue = "groq")
    public LLMService groqLLMService(LLMProperties properties) {
        return new GroqLLMService(properties);
    }
}

package com.chimera.service;

import com.chimera.model.GenerationRequest;
import com.chimera.model.GenerationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"chimera.llm.provider=mock"})
public class MockLLMServiceTest {

    @Autowired
    private LLMService llmService;

    @Test
    void testMockGeneration() {
        GenerationRequest request = new GenerationRequest(
            "You are a test assistant",
            "Say hello",
            Map.of()
        );

        GenerationResult result = llmService.generate(request).join();

        assertThat(result.text()).contains("[MOCK]");
        assertThat(result.finishReason()).isEqualTo("stop");
    }
}

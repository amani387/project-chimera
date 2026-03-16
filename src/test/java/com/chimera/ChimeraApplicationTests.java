package com.chimera;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "chimera.redis.enabled=false",
    "chimera.planner.enabled=false",
    "chimera.worker.enabled=false",
    "chimera.judge.enabled=false"
})
class ChimeraApplicationTests {

    @Test
    void contextLoads() {
    }

}

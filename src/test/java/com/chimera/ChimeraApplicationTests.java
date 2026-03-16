package com.chimera;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "chimera.planner.enabled=false")
class ChimeraApplicationTests {

	@Test
	void contextLoads() {
	}

}

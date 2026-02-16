package com.lockerroom.resourceservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SPRING_INTEGRATION_TEST", matches = "true")
class ResourceServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}

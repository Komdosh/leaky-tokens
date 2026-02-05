package com.leaky.tokens.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
    }
)
class ConfigServerApplicationTest {
    @Test
    void contextLoads() {
    }
}

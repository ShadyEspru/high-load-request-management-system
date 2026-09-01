package com.hlrms.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
        "spring.cloud.gateway.server.webflux.routes[0].id=test-route",
        "spring.cloud.gateway.server.webflux.routes[0].uri=http://localhost:9999",
        "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/test/**"
    }
)
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
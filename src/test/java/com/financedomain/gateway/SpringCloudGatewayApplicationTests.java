package com.financedomain.gateway;

import com.financedomain.gateway.filter.AuthenticationFilter;
import com.financedomain.gateway.filter.RouteValidator;
import com.financedomain.gateway.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "server.port=8080",
        "jwt.secret=NDBFRTYzNTI2NjU1NkE1ODZOMjcyMzU3NTM4N1g4MkY0MTNGOTQyODQ3MkI0QjYyNTA2NDUzNjc1NjZCNTk3MA==",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
class SpringCloudGatewayApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RouteValidator routeValidator;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Test
    @DisplayName("Vérifie le chargement du contexte Spring WebFlux et l'injection des beans du Gateway")
    void contextLoads() {
        assertNotNull(applicationContext, "Le contexte Spring Gateway doit être correctement initialisé.");
        assertThat(routeValidator).isNotNull();
        assertThat(jwtUtil).isNotNull();
        assertThat(authenticationFilter).isNotNull();
    }
}

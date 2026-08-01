package com.financedomain.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class RouteValidatorTest {

    private RouteValidator routeValidator;

    @BeforeEach
    void setUp() {
        routeValidator = new RouteValidator();
    }

    @Test
    @DisplayName("Devrait marquer les routes publiques (auth, inscription, eureka) comme non sécurisées (false)")
    void shouldMarkPublicEndpointsAsNotSecured() {
        ServerHttpRequest loginRequest = mockRequest("/auth/login");
        ServerHttpRequest registerRequest = mockRequest("/users/client/register");
        ServerHttpRequest eurekaRequest = mockRequest("/eureka/apps");

        assertFalse(routeValidator.isSecured.test(loginRequest));
        assertFalse(routeValidator.isSecured.test(registerRequest));
        assertFalse(routeValidator.isSecured.test(eurekaRequest));
    }

    @Test
    @DisplayName("Devrait marquer les routes privées comme sécurisées (true)")
    void shouldMarkPrivateEndpointsAsSecured() {
        ServerHttpRequest userProfileRequest = mockRequest("/users/me");
        ServerHttpRequest pricingRequest = mockRequest("/pricing/pass-internet");
        ServerHttpRequest transactionRequest = mockRequest("/transactions/transfer");

        assertTrue(routeValidator.isSecured.test(userProfileRequest));
        assertTrue(routeValidator.isSecured.test(pricingRequest));
        assertTrue(routeValidator.isSecured.test(transactionRequest));
    }

    private ServerHttpRequest mockRequest(String path) {
        ServerHttpRequest request = Mockito.mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080" + path));
        return request;
    }
}

package com.financedomain.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import com.financedomain.gateway.filter.AuthenticationFilter;

@Configuration
public class GatewayConfiguration {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Bean
    RouteLocator gatewayRouter(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("authentication-service", r -> r
                        .path("/auth/**")
                        .filters(f -> f.filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://authentication-service"))
                .route("user-service", r -> r
                        .path("/users/**")
                        .filters(f -> f.filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://user-service"))
                .build();
    }
}

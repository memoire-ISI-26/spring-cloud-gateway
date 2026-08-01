package com.financedomain.gateway.filter;

import com.financedomain.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    private RouteValidator validator;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        validator = new RouteValidator();
        authenticationFilter = new AuthenticationFilter(validator, jwtUtil);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("Devrait laisser passer les requêtes vers les routes publiques sans vérifier le token JWT")
    void shouldAllowPublicRouteWithoutHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("Devrait retourner 401 Unauthorized si l'en-tête Authorization est absent sur une route sécurisée")
    void shouldRejectSecuredRouteWithoutAuthHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/me").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("Devrait retourner 401 Unauthorized si le format de l'en-tête Bearer est invalide")
    void shouldRejectSecuredRouteWithInvalidBearerFormat() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic invalidtoken")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("Devrait retourner 401 Unauthorized si la session du token a expiré pour inactivité")
    void shouldRejectSecuredRouteWhenTokenExpiredOrInactive() {
        String token = "sampleToken123";
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.isTokenInactive(eq(token), anyLong())).thenReturn(true);

        StepVerifier.create(authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(jwtUtil).validateToken(token);
    }

    @Test
    @DisplayName("Devrait valider le token et injecter les en-têtes X-User-* lors d'un appel sécurisé valide")
    void shouldPassSecuredRouteAndMutateHeadersWhenTokenIsValid() {
        String token = "validToken123";
        MockServerHttpRequest request = MockServerHttpRequest.get("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims mockClaims = mock(Claims.class);
        when(mockClaims.get("id", String.class)).thenReturn("1");
        when(mockClaims.get("role", String.class)).thenReturn("CLIENT");
        when(mockClaims.getSubject()).thenReturn("771234567");

        doNothing().when(jwtUtil).validateToken(token);
        when(jwtUtil.isTokenInactive(eq(token), anyLong())).thenReturn(false);
        doNothing().when(jwtUtil).updateTokenActivity(eq(token), anyLong());
        when(jwtUtil.getClaims(token)).thenReturn(mockClaims);

        StepVerifier.create(authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        verify(jwtUtil).validateToken(token);
        verify(jwtUtil).updateTokenActivity(eq(token), anyLong());
        verify(chain).filter(any());
    }
}

package com.fightclub.setux.gateway.service;

import com.fightclub.setux.gateway.dto.responceDto.GatewayErrorResponse;
import com.fightclub.setux.gateway.dto.responceDto.GatewayRoute;
import com.fightclub.setux.gateway.exception.GatewayPolicyException;
import com.fightclub.setux.gateway.model.CompiledRoute;
import com.fightclub.setux.gateway.model.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class GatewayService {

    private final RouteRegistry routeRegistry;
    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;

    public GatewayService(RouteRegistry routeRegistry, ObjectMapper objectMapper,
                          RateLimitService rateLimitService) {
        this.routeRegistry = routeRegistry;
        this.objectMapper = objectMapper;
        this.rateLimitService = rateLimitService;
    }

    public ResponseEntity<byte[]> handle(HttpServletRequest request) {
        String correlationId = correlationId(request);
        Optional<CompiledRoute> matchedRoute = routeRegistry.find(request.getRequestURI());

        if (!matchedRoute.isPresent()) {
            return error(HttpStatus.NOT_FOUND, "No active route matches this request",
                    correlationId, null);
        }

        GatewayRoute route = matchedRoute.get().getGatewayRoute();

        if (!route.getAllowedMethods().contains(request.getMethod())) {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ALLOW, String.join(", ", route.getAllowedMethods()));

            return error(HttpStatus.METHOD_NOT_ALLOWED,
                    "HTTP method is not allowed for this route", correlationId, headers);
        }

        RateLimitResult rateLimit;
        try {
            rateLimit = rateLimitService.check(route, request.getRemoteAddr());
        } catch (GatewayPolicyException exception) {
            return error(exception.getStatus(), exception.getMessage(), correlationId, null);
        }

        HttpHeaders policyHeaders = rateLimitHeaders(rateLimit);

        if (!rateLimit.isAllowed()) {
            policyHeaders.set(HttpHeaders.RETRY_AFTER,
                    String.valueOf(rateLimit.getRetryAfterSeconds()));

            return error(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded",
                    correlationId, policyHeaders);
        }

        // Project 06 implements route-policy evaluation; target-service forwarding is added later.
        return error(HttpStatus.NOT_IMPLEMENTED,
                "Target-service forwarding is not implemented yet", correlationId, policyHeaders);
    }

    private String correlationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");

        if (correlationId == null || correlationId.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private HttpHeaders rateLimitHeaders(RateLimitResult result) {
        HttpHeaders headers = new HttpHeaders();

        if (result.getLimit() > 0) {
            headers.set("X-RateLimit-Limit", String.valueOf(result.getLimit()));
            headers.set("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        }
        return headers;
    }

    private ResponseEntity<byte[]> error(HttpStatus status, String message,
                                         String correlationId, HttpHeaders additionalHeaders) {
        GatewayErrorResponse response = new GatewayErrorResponse(status.value(), message,
                correlationId, LocalDateTime.now());

        HttpHeaders headers = additionalHeaders == null ? new HttpHeaders() : additionalHeaders;
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-ID", correlationId);

        try {
            return ResponseEntity.status(status).headers(headers)
                    .body(objectMapper.writeValueAsString(response)
                            .getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            String fallback = "{\"status\":" + status.value() + ",\"message\":\""
                    + message + "\",\"correlationId\":\"" + correlationId + "\"}";
            return ResponseEntity.status(status).headers(headers)
                    .body(fallback.getBytes(StandardCharsets.UTF_8));
        }
    }
}

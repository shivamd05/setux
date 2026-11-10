package com.fightclub.setux.management.dto.responceDto;

import com.fightclub.setux.management.dto.responceDto.HeaderRuleResponse;
import com.fightclub.setux.management.entity.ApiRoute;
import com.fightclub.setux.management.entity.RouteHeaderRule;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
public class RouteResponse {

    private Long id;
    private Long organizationId;
    private String routeId;
    private String name;
    private String pathPattern;
    private String targetBaseUrl;
    private String targetPathPrefix;
    private Integer requestsPerMinute;
    private Integer responseTimeoutMs;
    private boolean idempotencyEnabled;
    private boolean active;
    private Long configurationVersion;
    private Set<String> allowedMethods;
    private List<HeaderRuleResponse> headerRules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Converts a route entity and its rules into a response DTO. */
    public static RouteResponse from(ApiRoute route) {

        List<HeaderRuleResponse> rules = new ArrayList<>();

        for (RouteHeaderRule rule : route.getHeaderRules()) {
            rules.add(HeaderRuleResponse.from(rule));
        }

        return new RouteResponse(
                route.getId(),
                route.getOrganization().getId(),
                route.getRouteId(),
                route.getName(),
                route.getPathPattern(),
                route.getTargetBaseUrl(),
                route.getTargetPathPrefix(),
                route.getRequestsPerMinute(),
                route.getResponseTimeoutMs(),
                route.isIdempotencyEnabled(),
                route.isActive(),
                route.getConfigurationVersion(),
                route.getAllowedMethods(),
                rules,
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }
}

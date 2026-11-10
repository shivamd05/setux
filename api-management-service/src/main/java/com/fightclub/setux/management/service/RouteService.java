package com.fightclub.setux.management.service;

import com.fightclub.setux.management.dto.requestDto.HeaderRuleRequest;
import com.fightclub.setux.management.dto.requestDto.RouteRequest;
import com.fightclub.setux.management.dto.requestDto.RouteStatusRequest;
import com.fightclub.setux.management.dto.responceDto.AuditRecordResponse;
import com.fightclub.setux.management.dto.responceDto.RouteResponse;
import com.fightclub.setux.management.dto.responceDto.RouteVersionResponse;
import com.fightclub.setux.management.entity.ApiRoute;
import com.fightclub.setux.management.entity.ControlPlaneAuditRecord;
import com.fightclub.setux.management.entity.Organization;
import com.fightclub.setux.management.entity.OrganizationStatus;
import com.fightclub.setux.management.entity.RouteConfigurationVersion;
import com.fightclub.setux.management.entity.RouteHeaderRule;
import com.fightclub.setux.management.events.GatewayConfigurationEventService;
import com.fightclub.setux.management.exception.ApiException;
import com.fightclub.setux.management.repository.ApiRouteRepository;
import com.fightclub.setux.management.repository.ControlPlaneAuditRecordRepository;
import com.fightclub.setux.management.repository.RouteConfigurationVersionRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RouteService {

    private final ApiRouteRepository apiRouteRepository;

    private final TenantContext tenantContext;

    private final ObjectMapper objectMapper;
    private final RouteConfigurationVersionRepository versionRepository;
    private final ControlPlaneAuditRecordRepository auditRecordRepository;
    private final AuditService auditService;


    private final GatewayConfigurationEventService gatewayConfigurationEventService ;

    /** Creates the service with route, version, tenant, and audit dependencies. */
    public RouteService(
            ApiRouteRepository apiRouteRepository,
            TenantContext tenantContext,
            ObjectMapper objectMapper,
            RouteConfigurationVersionRepository versionRepository,
            ControlPlaneAuditRecordRepository auditRecordRepository,
            AuditService auditService,
            GatewayConfigurationEventService gatewayConfigurationEventService
                                        ) {
        this.apiRouteRepository = apiRouteRepository;
        this.tenantContext = tenantContext;
        this.objectMapper = objectMapper;
        this.versionRepository = versionRepository;
        this.auditRecordRepository = auditRecordRepository;
        this.auditService = auditService;
        this.gatewayConfigurationEventService =gatewayConfigurationEventService ;
    }

    /** Validates and creates a route with its initial version. */
    @Transactional
    public RouteResponse create(RouteRequest request) {
        long startedAt = System.nanoTime();

        Organization organization = tenantContext.getOrganization();
        validate(request, organization);

        if (apiRouteRepository.existsByOrganization_IdAndRouteId(
                organization.getId(), request.getRouteId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Route ID already exists for this organization");
        }

        ApiRoute route = new ApiRoute();
        route.setOrganization(organization);
        route.setConfigurationVersion(1L);
        route.setCreatedAt(LocalDateTime.now());

        apply(route, request);
        apiRouteRepository.save(route);

        saveVersion(route, "CREATED");

        auditService.record(
                organization,
                "CREATE_ROUTE",
                "API_ROUTE",
                route.getRouteId(),
                startedAt,
                "Route configuration version 1 created");

        gatewayConfigurationEventService.publish("CREATED", organization.getId(),
                route.getRouteId(), route.getConfigurationVersion());

        return RouteResponse.from(route);
    }

    /** Lists routes owned by the authenticated tenant. */
    @Transactional(readOnly = true)
    public List<RouteResponse> findAllForTenant() {
        Organization organization = tenantContext.getOrganization();

        return toResponses(
                apiRouteRepository.findAllByOrganization_Id(organization.getId()));
    }

    /** Returns a route only when it belongs to the authenticated tenant. */
    @Transactional(readOnly = true)
    public RouteResponse findById(Long id) {
        Organization organization = tenantContext.getOrganization();

        return RouteResponse.from(
                getOwnedRoute(id, organization.getId()));
    }

    /** Validates and saves a new version of a tenant-owned route. */
    @Transactional
    public RouteResponse update(Long id, RouteRequest request) {
        long startedAt = System.nanoTime();

        Organization organization = tenantContext.getOrganization();
        ApiRoute route = getOwnedRoute(id, organization.getId());

        if (!route.getRouteId().equals(request.getRouteId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "routeId cannot be changed");
        }

        validate(request, organization);

        route.setConfigurationVersion(
                route.getConfigurationVersion() + 1);

        apply(route, request);

        saveVersion(route, "UPDATED");

        auditService.record(
                organization,
                "UPDATE_ROUTE",
                "API_ROUTE",
                route.getRouteId(),
                startedAt,
                "Route configuration version "
                        + route.getConfigurationVersion() + " created");

        gatewayConfigurationEventService.publish("UPDATED", organization.getId(),
                route.getRouteId(), route.getConfigurationVersion());

        return RouteResponse.from(route);
    }

    /** Changes route activation state and records a new version. */
    @Transactional
    public RouteResponse changeStatus(Long id, RouteStatusRequest request) {
        long startedAt = System.nanoTime();

        Organization organization = tenantContext.getOrganization();
        ApiRoute route = getOwnedRoute(id, organization.getId());

        route.setActive(request.getActive());
        route.setConfigurationVersion(
                route.getConfigurationVersion() + 1);
        route.setUpdatedAt(LocalDateTime.now());

        String changeType = request.getActive()
                ? "ACTIVATED"
                : "DEACTIVATED";

        String action = request.getActive()
                ? "ACTIVATE_ROUTE"
                : "DEACTIVATE_ROUTE";

        saveVersion(route, changeType);

        auditService.record(
                organization,
                action,
                "API_ROUTE",
                route.getRouteId(),
                startedAt,
                "Route configuration version "
                        + route.getConfigurationVersion() + " created");

        gatewayConfigurationEventService.publish(changeType, organization.getId(),
                route.getRouteId(), route.getConfigurationVersion());

        return RouteResponse.from(route);
    }

    /** Records the final route version and deletes the route. */
    @Transactional
    public void delete(Long id) {
        long startedAt = System.nanoTime();

        Organization organization = tenantContext.getOrganization();
        ApiRoute route = getOwnedRoute(id, organization.getId());

        route.setConfigurationVersion(
                route.getConfigurationVersion() + 1);
        route.setUpdatedAt(LocalDateTime.now());

        saveVersion(route, "DELETED");

        auditService.record(
                organization,
                "DELETE_ROUTE",
                "API_ROUTE",
                route.getRouteId(),
                startedAt,
                "Final configuration saved as version "
                        + route.getConfigurationVersion());
        gatewayConfigurationEventService.publish("DELETED", organization.getId(),
                route.getRouteId(), route.getConfigurationVersion());


        apiRouteRepository.delete(route);
    }

    /** Returns saved versions for a route in the current tenant. */
    @Transactional(readOnly = true)
    public List<RouteVersionResponse> getVersions(String routeId) {
        Organization organization = tenantContext.getOrganization();

        List<RouteVersionResponse> response = new ArrayList<>();

        List<RouteConfigurationVersion> versions =
                versionRepository
                        .findAllByOrganizationIdAndRouteIdOrderByVersionNumberDesc(
                                organization.getId(), routeId);

        for (RouteConfigurationVersion version : versions) {
            response.add(RouteVersionResponse.from(version));
        }

        return response;
    }

    /** Lists active routes in active organizations for the gateway. */
    @Transactional(readOnly = true)
    public List<RouteResponse> findAllActiveForGateway() {
        return toResponses(
                apiRouteRepository.findAllByActiveTrueAndOrganization_Status(
                        OrganizationStatus.ACTIVE));
    }

    /** Loads a route only when it belongs to the specified tenant. */
    private ApiRoute getOwnedRoute(Long id, Long organizationId) {
        return apiRouteRepository
                .findByIdAndOrganization_Id(id, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Route not found"));
    }

    /** Copies validated request values and header rules onto a route. */
    private void apply(ApiRoute route, RouteRequest request) {
        route.setRouteId(request.getRouteId());
        route.setName(request.getName());
        route.setPathPattern(request.getPathPattern());
        route.setTargetBaseUrl(request.getTargetBaseUrl());
        route.setTargetPathPrefix(request.getTargetPathPrefix());
        route.setRequestsPerMinute(request.getRequestsPerMinute());
        route.setResponseTimeoutMs(request.getResponseTimeoutMs());
        route.setIdempotencyEnabled(request.isIdempotencyEnabled());
        route.setActive(request.isActive());
        route.setUpdatedAt(LocalDateTime.now());
        route.setAllowedMethods(normalizeMethods(request.getAllowedMethods()));

        route.getHeaderRules().clear();

        for (HeaderRuleRequest ruleRequest : request.getHeaderRules()) {
            RouteHeaderRule rule = new RouteHeaderRule();

            rule.setRoute(route);
            rule.setDirection(
                    ruleRequest.getDirection().toUpperCase(Locale.ENGLISH));
            rule.setAction(
                    ruleRequest.getAction().toUpperCase(Locale.ENGLISH));
            rule.setHeaderName(ruleRequest.getHeaderName());
            rule.setHeaderValue(ruleRequest.getHeaderValue());
            rule.setCreatedAt(LocalDateTime.now());
            rule.setUpdatedAt(LocalDateTime.now());

            route.getHeaderRules().add(rule);
        }
    }

    /** Checks tenant path, target URL, methods, and header rules. */
    private void validate(RouteRequest request, Organization organization) {
        String tenantPrefix = "/" + organization.getSlug() + "/";

        if (!request.getPathPattern().startsWith(tenantPrefix)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "pathPattern must start with the tenant prefix "
                            + tenantPrefix);
        }

        if (!request.getTargetPathPrefix().startsWith("/")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "targetPathPrefix must start with /");
        }

        try {
            URI uri = URI.create(request.getTargetBaseUrl());

            if (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "targetBaseUrl must be a valid HTTP or HTTPS URL");
        }

        normalizeMethods(request.getAllowedMethods());

        for (HeaderRuleRequest rule : request.getHeaderRules()) {
            String direction =
                    rule.getDirection().toUpperCase(Locale.ENGLISH);

            String action =
                    rule.getAction().toUpperCase(Locale.ENGLISH);

            if (!"REQUEST".equals(direction)
                    && !"RESPONSE".equals(direction)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Header direction must be REQUEST or RESPONSE");
            }

            if (!"ADD".equals(action) && !"REMOVE".equals(action)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Header action must be ADD or REMOVE");
            }

            if ("ADD".equals(action)
                    && (rule.getHeaderValue() == null
                    || rule.getHeaderValue().trim().isEmpty())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "ADD header rules require headerValue");
            }
        }
    }

    /** Validates HTTP methods and normalizes them to uppercase. */
    private Set<String> normalizeMethods(Set<String> methods) {
        Set<String> normalized = new HashSet<>();

        for (String method : methods) {
            String value = method.toUpperCase(Locale.ENGLISH);

            try {
                HttpMethod.valueOf(value);
            } catch (IllegalArgumentException exception) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported HTTP method: " + method);
            }

            normalized.add(value);
        }

        return normalized;
    }

    /** Stores a snapshot of the current route configuration. */
    private void saveVersion(ApiRoute route, String changeType) {
        // Store a snapshot so clients can inspect each route configuration revision.
        RouteConfigurationVersion version =
                new RouteConfigurationVersion();

        version.setOrganization(route.getOrganization());
        version.setRouteId(route.getRouteId());
        version.setVersionNumber(route.getConfigurationVersion());
        version.setChangeType(changeType);
        version.setConfigurationSnapshot(
                objectMapper.writeValueAsString(RouteResponse.from(route)));
        version.setCreatedAt(LocalDateTime.now());

        versionRepository.save(version);
    }

    /** Converts route entities into response DTOs. */
    private List<RouteResponse> toResponses(List<ApiRoute> routes) {
        List<RouteResponse> response = new ArrayList<>();

        for (ApiRoute route : routes) {
            response.add(RouteResponse.from(route));
        }

        return response;
    }

    /** Returns audit history for the authenticated tenant. */
    @Transactional(readOnly = true)
    public List<AuditRecordResponse> getAuditRecords() {
        Organization organization = tenantContext.getOrganization();

        List<AuditRecordResponse> response = new ArrayList<>();

        List<ControlPlaneAuditRecord> records =
                auditRecordRepository
                        .findAllByOrganizationIdOrderByCreatedAtDesc(
                                organization.getId());

        for (ControlPlaneAuditRecord record : records) {
            response.add(AuditRecordResponse.from(record));
        }

        return response;
    }
}

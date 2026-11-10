package com.fightclub.setux.management.controller;

import com.fightclub.setux.management.dto.requestDto.RouteRequest;
import com.fightclub.setux.management.dto.requestDto.RouteStatusRequest;
import com.fightclub.setux.management.dto.responceDto.AuditRecordResponse;
import com.fightclub.setux.management.dto.responceDto.RouteResponse;
import com.fightclub.setux.management.dto.responceDto.RouteVersionResponse;
import com.fightclub.setux.management.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant")
public class TenantRouteController {

    private final RouteService routeService;

    /** Creates the controller with its route service. */
    public TenantRouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    /** Creates a route for the authenticated tenant. */
    @PostMapping("/routes")
    public ResponseEntity<RouteResponse> create(
            @Valid @RequestBody RouteRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.create(request));
    }

    /** Lists routes owned by the authenticated tenant. */
    @GetMapping("/routes")
    public ResponseEntity<List<RouteResponse>> findAll() {
        return ResponseEntity.ok(routeService.findAllForTenant());
    }

    /** Returns a tenant-owned route by its ID. */
    @GetMapping("/routes/{id}")
    public ResponseEntity<RouteResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.findById(id));
    }

    /** Updates a tenant-owned route. */
    @PutMapping("/routes/{id}")
    public ResponseEntity<RouteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RouteRequest request) {

        return ResponseEntity.ok(routeService.update(id, request));
    }

    /** Activates or deactivates a tenant-owned route. */
    @PatchMapping("/routes/{id}/status")
    public ResponseEntity<RouteResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody RouteStatusRequest request) {

        return ResponseEntity.ok(routeService.changeStatus(id, request));
    }

    /** Deletes a tenant-owned route. */
    @DeleteMapping("/routes/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        routeService.delete(id);

        return ResponseEntity.noContent().build();
    }

    /** Returns saved configuration history for one tenant-owned route. */
    @GetMapping("/route-versions/{routeId}")
    public ResponseEntity<List<RouteVersionResponse>> getVersions(
            @PathVariable String routeId) {

        return ResponseEntity.ok(routeService.getVersions(routeId));
    }

    /** Returns audit records for the authenticated tenant. */
    @GetMapping("/audit-records")
    public ResponseEntity<List<AuditRecordResponse>> getAuditRecords() {
        return ResponseEntity.ok(routeService.getAuditRecords());
    }
}

package com.fightclub.setux.management.controller;

import com.fightclub.setux.management.dto.requestDto.OrganizationRequest;
import com.fightclub.setux.management.dto.requestDto.OrganizationStatusRequest;
import com.fightclub.setux.management.dto.requestDto.TenantAdminRequest;
import com.fightclub.setux.management.dto.responceDto.OrganizationResponce;
import com.fightclub.setux.management.dto.responceDto.TenantAdminResponse;
import com.fightclub.setux.management.service.OrganizationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/organizations")
public class PlatformOrganizationController {

    private final OrganizationService organizationService;

    /** Creates the controller with its organization service. */
    public PlatformOrganizationController(
            OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /** Creates an organization. */
    @PostMapping
    public ResponseEntity<OrganizationResponce> create(
            @Valid @RequestBody OrganizationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.create(request));
    }

    /** Lists organizations. */
    @GetMapping
    public ResponseEntity<List<OrganizationResponce>> findAll() {

        return ResponseEntity.ok(organizationService.findAll());
    }

    /** Finds an organization by its ID. */
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponce> findById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                organizationService.findById(id));
    }

    /** Updates an organization. */
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponce> update(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationRequest request) {

        return ResponseEntity.ok(
                organizationService.update(id, request));
    }

    /** Changes an organization status. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrganizationResponce> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrganizationStatusRequest request) {

        return ResponseEntity.ok(
                organizationService.changeStatus(id, request));
    }

    /** Soft-deletes an organization. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        organizationService.delete(id);

        return ResponseEntity.noContent().build();
    }

    // CRUD --> Tenant Admins

    /** Creates a tenant administrator for an organization. */
    @PostMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<TenantAdminResponse> createTenantAdmin(
            @PathVariable Long organizationId,
            @Valid @RequestBody TenantAdminRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createTenantAdmin(organizationId, request));
    }

    /** Returns an organization tenant administrator. */
    @GetMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<TenantAdminResponse> getTenantAdmin(
            @PathVariable Long organizationId) {

        return ResponseEntity.ok(
                organizationService.getTenantAdmin(organizationId));
    }
    /** Updates an organization tenant administrator. */
    @PutMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<TenantAdminResponse> updateTenantAdmin(
            @PathVariable Long organizationId,
            @Valid @RequestBody TenantAdminRequest request) {

        return ResponseEntity.ok(
                organizationService.updateTenantAdmin(organizationId, request));
    }

    /** Deletes an organization tenant administrator. */
    @DeleteMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<Void> deleteTenantAdmin(
            @PathVariable Long organizationId) {

        organizationService.deleteTenantAdmin(organizationId);

        return ResponseEntity.noContent().build();
    }
}

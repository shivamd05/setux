package com.fightclub.setux.management.controller;

import com.fightclub.setux.management.dto.requestDto.TenantOrganizationRequest;
import com.fightclub.setux.management.dto.responceDto.OrganizationResponce;
import com.fightclub.setux.management.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/organization")
public class TenantOrganizationController {

    private final OrganizationService organizationService;

    /** Creates the controller with its organization service. */
    public TenantOrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /** Returns the authenticated tenant organization. */
    @GetMapping
    public ResponseEntity<OrganizationResponce> getOwnOrganization() {
        return ResponseEntity.ok(organizationService.getOwnOrganization());
    }

    /** Updates the authenticated tenant organization. */
    @PutMapping
    public ResponseEntity<OrganizationResponce> updateOwnOrganization(
            @Valid @RequestBody TenantOrganizationRequest request) {
        return ResponseEntity.ok(
                organizationService.updateOwnOrganization(request));
    }
}

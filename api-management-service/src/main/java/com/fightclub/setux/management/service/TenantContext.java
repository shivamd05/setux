package com.fightclub.setux.management.service;

import com.fightclub.setux.management.entity.Organization;
import com.fightclub.setux.management.entity.OrganizationStatus;
import com.fightclub.setux.management.exception.ApiException;
import com.fightclub.setux.management.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class TenantContext {

    private final OrganizationRepository organizationRepository;

    /** Creates the tenant context with its organization repository. */
    public TenantContext(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /** Loads the active organization from the authenticated JWT. */
    public Organization getOrganization() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tenant identity is missing");
        }

        Long organizationId = ((JwtAuthenticationToken) authentication).getToken()
                .getClaim("organizationId");

        if (organizationId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tenant identity is missing");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.FORBIDDEN,
                        "Organization does not exist"
                ));

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "The organization is not active"
            );
        }

        return organization;
    }
}

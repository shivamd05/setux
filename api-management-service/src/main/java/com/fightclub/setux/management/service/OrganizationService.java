package com.fightclub.setux.management.service;

import com.fightclub.setux.management.dto.requestDto.TenantOrganizationRequest;
import com.fightclub.setux.management.dto.requestDto.OrganizationRequest;
import com.fightclub.setux.management.dto.requestDto.OrganizationStatusRequest;
import com.fightclub.setux.management.dto.requestDto.TenantAdminRequest;
import com.fightclub.setux.management.dto.responceDto.OrganizationResponce;
import com.fightclub.setux.management.dto.responceDto.TenantAdminResponse;
import com.fightclub.setux.management.entity.ManagementUser;
import com.fightclub.setux.management.entity.Organization;
import com.fightclub.setux.management.entity.OrganizationStatus;
import com.fightclub.setux.management.entity.Role;
import com.fightclub.setux.management.events.GatewayConfigurationEventService;
import com.fightclub.setux.management.exception.ApiException;
import com.fightclub.setux.management.repository.ManagementUserRepository;
import com.fightclub.setux.management.repository.OrganizationRepository;
import com.fightclub.setux.management.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ManagementUserRepository managementUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final GatewayConfigurationEventService gatewayConfigurationEventService ;

    /** Creates the service with its organization, user, role, and audit dependencies. */
    public OrganizationService(
            OrganizationRepository organizationRepository,
            ManagementUserRepository managementUserRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            TenantContext tenantContext,
            AuditService auditService ,
            GatewayConfigurationEventService gatewayConfigurationEventService) {

        this.organizationRepository = organizationRepository;
        this.managementUserRepository = managementUserRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
        this.gatewayConfigurationEventService = gatewayConfigurationEventService ;
    }

    /** Creates a pending organization and records the action. */
    @Transactional
    public OrganizationResponce create(OrganizationRequest request) {

        long startedAt = System.nanoTime();

        checkUnique(request.getName(), request.getSlug(), null);

        Organization organization = new Organization();

        organization.setName(request.getName());
        organization.setSlug(request.getSlug());
        organization.setStatus(OrganizationStatus.PENDING);
        organization.setCreatedAt(LocalDateTime.now());
        organization.setUpdatedAt(LocalDateTime.now());

        organizationRepository.save(organization);

        auditService.record(organization, "CREATE_ORGANIZATION", "ORGANIZATION",
                organization.getId().toString(), startedAt,
                "Organization created in PENDING status");

        return OrganizationResponce.from(organization);
    }

    /** Rejects names or slugs already used by another organization. */
    public void checkUnique(String name, String slug, Long currentId) {

        if (organizationRepository.existsByNameIgnoreCase(name)) {

            Organization current = currentId == null
                    ? null
                    : organizationRepository.findById(currentId).orElse(null);

            if (current == null
                    || !current.getName().equalsIgnoreCase(name)) {

                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Organization name already exists"
                );
            }
        }

        if (organizationRepository.existsBySlugIgnoreCase(slug)) {

            Organization current = currentId == null
                    ? null
                    : organizationRepository.findById(currentId).orElse(null);

            if (current == null
                    || !current.getSlug().equalsIgnoreCase(slug)) {

                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Organization slug already exists"
                );
            }
        }
    }

    /** Lists organizations that have not been deleted. */
    @Transactional
    public List<OrganizationResponce> findAll() {

        List<OrganizationResponce> responses = new ArrayList<>();

        for (Organization organization :
                organizationRepository.findAllByStatusNot(
                        OrganizationStatus.DELETED)) {

            responses.add(OrganizationResponce.from(organization));
        }

        return responses;
    }

    /** Returns an organization by ID unless it has been deleted. */
    @Transactional
    public OrganizationResponce findById(Long id) {

        return OrganizationResponce.from(getOrganization(id));
    }

    /** Updates an organization name and slug. */
    @Transactional
    public OrganizationResponce update(
            Long id,
            OrganizationRequest request) {

        long startedAt = System.nanoTime();

        Organization organization = getOrganization(id);

        checkUnique(request.getName(), request.getSlug(), id);

        organization.setName(request.getName());
        organization.setSlug(request.getSlug());
        organization.setUpdatedAt(LocalDateTime.now());

        auditService.record(organization, "UPDATE_ORGANIZATION", "ORGANIZATION",
                id.toString(), startedAt, "Organization name and slug updated");

        gatewayConfigurationEventService.publish(
                "ORGANIZATION_UPDATED",
                organization.getId(),
                null,
                null
        );

        return OrganizationResponce.from(organization);
    }

    /** Changes an organization status. */
    @Transactional
    public OrganizationResponce changeStatus(
            Long id,
            OrganizationStatusRequest request) {

        long startedAt = System.nanoTime();

        Organization organization = getOrganization(id);

        if (request.getStatus() == OrganizationStatus.DELETED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Use Delete to soft delete organization"
            );
        }

        organization.setStatus(request.getStatus());
        organization.setUpdatedAt(LocalDateTime.now());

        auditService.record(organization, "CHANGE_ORGANIZATION_STATUS", "ORGANIZATION",
                id.toString(), startedAt, "Status changed to " + request.getStatus());

        gatewayConfigurationEventService.publish(
                "ORGANIZATION_" + request.getStatus(),
                organization.getId(),
                null,
                null);

        return OrganizationResponce.from(organization);
    }

    /** Soft-deletes an organization. */
    @Transactional
    public void delete(Long id) {

        long startedAt = System.nanoTime();

        Organization organization = getOrganization(id);

        organization.setStatus(OrganizationStatus.DELETED);
        organization.setDeletedAt(LocalDateTime.now());
        organization.setUpdatedAt(LocalDateTime.now());

        auditService.record(organization, "DELETE_ORGANIZATION", "ORGANIZATION",
                id.toString(), startedAt, "Organization soft-deleted");

        gatewayConfigurationEventService.publish(
                "ORGANIZATION_DELETED",
                organization.getId(),
                null,
                null);
    }

    /** Loads an organization that has not been deleted. */
    private Organization getOrganization(Long id) {

        return organizationRepository
                .findByIdAndStatusNot(id, OrganizationStatus.DELETED)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Organization not found"
                ));
    }

    /** Creates the tenant administrator for an organization. */
    @Transactional
    public TenantAdminResponse createTenantAdmin(
            Long organizationId,
            TenantAdminRequest request) {

        long startedAt = System.nanoTime();

        Organization organization = getOrganization(organizationId);

        if (managementUserRepository
                .findByOrganizationId(organizationId).isPresent()) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "This organization already has a tenant administrator"
            );
        }

        if (managementUserRepository
                .existsByUsername(request.getUsername())) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        Role role = roleRepository.findByName("TENANT_ADMIN")
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "TENANT_ADMIN role is missing"
                ));

        ManagementUser user = new ManagementUser();

        user.setOrganization(organization);
        user.setRole(role);
        user.setUsername(request.getUsername());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setEnabled(request.isEnabled());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        managementUserRepository.save(user);

        auditService.record(organization, "CREATE_TENANT_ADMIN", "MANAGEMENT_USER",
                user.getId().toString(), startedAt, "Tenant administrator created");

        return TenantAdminResponse.from(user);
    }
    /** Returns an organization tenant administrator. */
    @Transactional(readOnly = true)
    public TenantAdminResponse getTenantAdmin(Long organizationId) {
        return TenantAdminResponse.from(
                getTenantAdminEntity(organizationId)
        );
    }

    /** Updates an organization tenant administrator. */
    @Transactional
    public TenantAdminResponse updateTenantAdmin(
            Long organizationId,
            TenantAdminRequest request) {

        long startedAt = System.nanoTime();

        ManagementUser user = getTenantAdminEntity(organizationId);

        if (!user.getUsername().equals(request.getUsername())
                && managementUserRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        user.setUsername(request.getUsername());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setEnabled(request.isEnabled());
        user.setUpdatedAt(LocalDateTime.now());

        ManagementUser updatedUser = managementUserRepository.save(user);

        auditService.record(user.getOrganization(), "UPDATE_TENANT_ADMIN", "MANAGEMENT_USER",
                user.getId().toString(), startedAt, "Tenant administrator updated");

        return TenantAdminResponse.from(updatedUser);
    }

    /** Deletes an organization tenant administrator. */
    @Transactional
    public void deleteTenantAdmin(Long organizationId) {
        long startedAt = System.nanoTime();

        ManagementUser user = getTenantAdminEntity(organizationId);

        auditService.record(user.getOrganization(), "DELETE_TENANT_ADMIN", "MANAGEMENT_USER",
                user.getId().toString(), startedAt, "Tenant administrator deleted");

        managementUserRepository.delete(user);
    }

    /** Loads the tenant administrator for an active organization. */
    private ManagementUser getTenantAdminEntity(Long organizationId) {
        getOrganization(organizationId);

        return managementUserRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Tenant admin not found"
                ));
    }

    /** Returns the authenticated tenant organization. */
    @Transactional(readOnly = true)
    public OrganizationResponce getOwnOrganization() {
        return OrganizationResponce.from(tenantContext.getOrganization()) ;

    }

    /** Updates the authenticated tenant organization name. */
    @Transactional
    public OrganizationResponce updateOwnOrganization(
            TenantOrganizationRequest request) {

        long startedAt = System.nanoTime();

        Organization organization = tenantContext.getOrganization();

        if (!organization.getName().equalsIgnoreCase(request.getName())
                && organizationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Organization name already exists"
            );
        }

        organization.setName(request.getName());
        organization.setUpdatedAt(LocalDateTime.now());

        auditService.record(organization, "UPDATE_OWN_ORGANIZATION", "ORGANIZATION",
                organization.getId().toString(), startedAt,
                "Tenant updated its organization name");

        return OrganizationResponce.from(organization);
    }

}

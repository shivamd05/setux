package com.fightclub.setux.management.repository;

import com.fightclub.setux.management.entity.ApiRoute;
import com.fightclub.setux.management.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRouteRepository extends JpaRepository<ApiRoute, Long> {

    /** Finds routes owned by the specified organization. */
    List<ApiRoute> findAllByOrganization_Id(Long organizationId);

    /** Finds active routes whose organization has the supplied status. */
    List<ApiRoute> findAllByActiveTrueAndOrganization_Status(
            OrganizationStatus status
    );

    /** Finds a route only when it belongs to the specified organization. */
    Optional<ApiRoute> findByIdAndOrganization_Id(
            Long id,
            Long organizationId
    );

    /** Checks whether an organization already uses the route ID. */
    boolean existsByOrganization_IdAndRouteId(
            Long organizationId,
            String routeId
    );
}

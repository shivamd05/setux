package com.fightclub.setux.management.repository;

import com.fightclub.setux.management.entity.RouteConfigurationVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteConfigurationVersionRepository
        extends JpaRepository<RouteConfigurationVersion, Long> {

    /** Lists one route's versions from newest to oldest within an organization. */
    List<RouteConfigurationVersion>
    findAllByOrganizationIdAndRouteIdOrderByVersionNumberDesc(
            Long organizationId,
            String routeId);
}

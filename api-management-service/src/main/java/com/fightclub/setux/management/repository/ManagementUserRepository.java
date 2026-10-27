package com.fightclub.setux.management.repository;

import com.fightclub.setux.management.entity.ManagementUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagementUserRepository
        extends JpaRepository<ManagementUser, Long> {

    /** Finds a management user by username. */
    Optional<ManagementUser> findByUsername(String username);

    /** Finds a tenant administrator by organization ID. */
    Optional<ManagementUser> findByOrganizationId(Long organizationId);

    /** Checks whether a username is already registered. */
    boolean existsByUsername(String username);
}

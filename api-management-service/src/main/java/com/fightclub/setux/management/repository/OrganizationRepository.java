package com.fightclub.setux.management.repository;

import com.fightclub.setux.management.entity.Organization;
import com.fightclub.setux.management.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    /** Checks whether an organization name is already used, ignoring case. */
    boolean existsByNameIgnoreCase(String name);

    /** Checks whether an organization slug is already used, ignoring case. */
    boolean existsBySlugIgnoreCase(String slug);

    /** Lists organizations whose status differs from the supplied status. */
    List<Organization> findAllByStatusNot(OrganizationStatus status);

    /** Finds an organization unless it has the excluded status. */
    Optional<Organization> findByIdAndStatusNot(Long id, OrganizationStatus status);
}

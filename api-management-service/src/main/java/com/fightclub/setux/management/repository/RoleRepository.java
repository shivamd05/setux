package com.fightclub.setux.management.repository;

import com.fightclub.setux.management.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /** Finds a role by its name. */
    Optional<Role> findByName(String name);
}

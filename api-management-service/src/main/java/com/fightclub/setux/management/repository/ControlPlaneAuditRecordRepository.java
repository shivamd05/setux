package com.fightclub.setux.management.repository;

import com.fightclub.setux.management.entity.ControlPlaneAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlPlaneAuditRecordRepository
        extends JpaRepository<ControlPlaneAuditRecord, Long> {

    // Keep a tenant's audit history newest first, matching the tutorial endpoint.
    List<ControlPlaneAuditRecord> findAllByOrganizationIdOrderByCreatedAtDesc(
            Long organizationId);
}

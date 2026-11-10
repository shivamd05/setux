package com.fightclub.setux.management.service;

import com.fightclub.setux.management.entity.ControlPlaneAuditRecord;
import com.fightclub.setux.management.entity.Organization;
import com.fightclub.setux.management.repository.ControlPlaneAuditRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final ControlPlaneAuditRecordRepository auditRecordRepository;

    /** Creates the service with its audit-record repository. */
    public AuditService(ControlPlaneAuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    /** Persists an audit record for a completed management action. */
    public void record(Organization organization, String action,
                       String resourceType, String resourceId,
                       long startedAt, String details) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null ? "system" : authentication.getName();

        ControlPlaneAuditRecord record = new ControlPlaneAuditRecord();
        record.setOrganization(organization);
        record.setActor(actor);
        record.setAction(action);
        record.setResourceType(resourceType);
        record.setResourceId(resourceId);
        record.setCorrelationId(getCorrelationId());
        record.setDurationMs((System.nanoTime() - startedAt) / 1_000_000);
        record.setResult("SUCCESS");
        record.setDetails(details);
        record.setCreatedAt(LocalDateTime.now());

        auditRecordRepository.save(record);
    }

    /** Reads the request correlation ID when a web request is active. */
    private String getCorrelationId() {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader("X-Correlation-ID");
    }
}

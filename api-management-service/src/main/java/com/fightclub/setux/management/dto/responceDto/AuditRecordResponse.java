package com.fightclub.setux.management.dto.responceDto;

import com.fightclub.setux.management.entity.ControlPlaneAuditRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuditRecordResponse {

    private Long id;
    private String actor;
    private String action;
    private String resourceType;
    private String resourceId;
    private String correlationId;
    private Long durationMs;
    private String result;
    private String details;
    private LocalDateTime createdAt;

    /** Converts an audit entity into its response DTO. */
    public static AuditRecordResponse from(ControlPlaneAuditRecord record) {
        return new AuditRecordResponse(
                record.getId(), record.getActor(), record.getAction(),
                record.getResourceType(), record.getResourceId(),
                record.getCorrelationId(), record.getDurationMs(),
                record.getResult(), record.getDetails(), record.getCreatedAt());
    }
}

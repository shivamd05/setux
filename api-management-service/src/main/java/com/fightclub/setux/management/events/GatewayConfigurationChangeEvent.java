package com.fightclub.setux.management.events;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class GatewayConfigurationChangeEvent {

    private String eventId;
    private String eventType;
    private String changeType;
    private Long organizationId;
    private String routeId;
    private Long configurationVersion;
    private LocalDateTime occurredAt;
}
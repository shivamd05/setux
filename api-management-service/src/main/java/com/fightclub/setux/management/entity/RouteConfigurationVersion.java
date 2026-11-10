package com.fightclub.setux.management.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_configuration_versions")
@Getter
@Setter
@NoArgsConstructor
public class RouteConfigurationVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    private String routeId;

    private Long versionNumber;

    private String changeType;

    @Column(columnDefinition = "LONGTEXT")
    private String configurationSnapshot;

    private LocalDateTime createdAt;
}

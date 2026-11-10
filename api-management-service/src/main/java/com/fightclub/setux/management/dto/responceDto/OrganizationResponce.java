package com.fightclub.setux.management.dto.responceDto;

import com.fightclub.setux.management.entity.Organization;
import com.fightclub.setux.management.entity.OrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrganizationResponce {

    private long id;
    private String name;
    private String slug;
    private OrganizationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Converts an organization entity into its response DTO. */
    public static OrganizationResponce from(Organization organization) {
        return new OrganizationResponce(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getStatus(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }

}

package com.fightclub.setux.management.dto.responceDto;

import com.fightclub.setux.management.entity.ManagementUser;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TenantAdminResponse {

    private Long id;
    private Long organizationId;
    private String username;
    private boolean enabled;

    /** Converts a management user into a tenant-admin response DTO. */
    public static TenantAdminResponse from(ManagementUser user) {
        return new TenantAdminResponse(
                user.getId(),
                user.getOrganization().getId(),
                user.getUsername(),
                user.isEnabled()
        );
    }
}

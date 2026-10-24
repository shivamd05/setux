
package com.fightclub.setux.management.dto.responceDto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponce {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String role;
    private Long organizationId;
}

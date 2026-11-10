package com.fightclub.setux.gateway.dto.responceDto;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class LoginRequest {

    private String username;
    private String password;
}

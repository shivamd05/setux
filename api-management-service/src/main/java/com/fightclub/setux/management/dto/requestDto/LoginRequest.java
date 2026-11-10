package com.fightclub.setux.management.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    // Reject empty usernames as well as missing values.
    @NotBlank
    private String username;

    @NotBlank
    private String password;

}

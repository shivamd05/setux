package com.fightclub.setux.management.controller;

import com.fightclub.setux.management.dto.requestDto.LoginRequest;
import com.fightclub.setux.management.dto.responceDto.LoginResponce;
import com.fightclub.setux.management.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /** Creates the controller with its authentication service. */
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /** Authenticates the supplied credentials and returns a JWT. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponce> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }
}

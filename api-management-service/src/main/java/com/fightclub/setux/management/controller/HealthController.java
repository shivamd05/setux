package com.fightclub.setux.management.controller;

import com.fightclub.setux.management.dto.responceDto.HealthResponce;
import com.fightclub.setux.management.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<HealthResponce> health() {
        String dbStatus = healthService.getHealth();

        return ResponseEntity.ok(
                new HealthResponce("UP", dbStatus, LocalDateTime.now())
        );
    }
}
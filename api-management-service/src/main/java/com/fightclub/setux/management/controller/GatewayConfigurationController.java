package com.fightclub.setux.management.controller;

import com.fightclub.setux.management.dto.responceDto.RouteResponse;
import com.fightclub.setux.management.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gateway/routes")
public class GatewayConfigurationController {

    private final RouteService routeService;

    /** Creates the controller with its route service. */
    public GatewayConfigurationController(RouteService routeService) {
        this.routeService = routeService;
    }

    /** Returns active routes that the gateway can load. */
    @GetMapping
    public ResponseEntity<List<RouteResponse>> getActiveRoutes() {
        return ResponseEntity.ok(routeService.findAllActiveForGateway());
    }
}

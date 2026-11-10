package com.fightclub.setux.gateway.service;

import com.fightclub.setux.gateway.client.ManagementApiClient;
import com.fightclub.setux.gateway.dto.responceDto.GatewayRoute;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GatewayConfigurationLoader implements ApplicationRunner {

    private final ManagementApiClient managementApiClient;

    public GatewayConfigurationLoader(ManagementApiClient managementApiClient) {
        this.managementApiClient = managementApiClient;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        refresh();
    }

    public void refresh() {
        List<GatewayRoute> routes = managementApiClient.fetchActiveRoutes();

        for (GatewayRoute route : routes) {
            System.out.println("Route : " + route.getTargetPathPrefix());
        }
    }
}

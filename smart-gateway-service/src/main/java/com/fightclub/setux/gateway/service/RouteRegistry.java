package com.fightclub.setux.gateway.service;

import com.fightclub.setux.gateway.dto.responceDto.GatewayRoute;
import com.fightclub.setux.gateway.model.CompiledRoute;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RouteRegistry {

    private final PathPatternParser parser = new PathPatternParser();
    private final AtomicReference<List<CompiledRoute>> routes =
            new AtomicReference<List<CompiledRoute>>(Collections.<CompiledRoute>emptyList());

    public void replaceAll(List<GatewayRoute> gatewayRoutes) {
        List<CompiledRoute> compiledRoutes = new ArrayList<CompiledRoute>();

        if (gatewayRoutes != null) {
            for (GatewayRoute route : gatewayRoutes) {
                PathPattern pattern = parser.parse(route.getPathPattern());
                compiledRoutes.add(new CompiledRoute(route, pattern,
                        sourcePrefix(route.getPathPattern())));
            }
        }

        Collections.sort(compiledRoutes, new Comparator<CompiledRoute>() {
            @Override
            public int compare(CompiledRoute first, CompiledRoute second) {
                return PathPattern.SPECIFICITY_COMPARATOR.compare(
                        first.getPathPattern(), second.getPathPattern());
            }
        });

        routes.set(Collections.unmodifiableList(compiledRoutes));
    }

    public Optional<CompiledRoute> find(String requestPath) {
        PathContainer path = PathContainer.parsePath(requestPath);

        for (CompiledRoute route : routes.get()) {
            if (route.getPathPattern().matches(path)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    private String sourcePrefix(String pathPattern) {
        if (pathPattern.endsWith("/**")) {
            return pathPattern.substring(0, pathPattern.length() - 3);
        }
        return pathPattern;
    }
}

package com.fightclub.setux.gateway.model;

import com.fightclub.setux.gateway.dto.responceDto.GatewayRoute;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.util.pattern.PathPattern;

@Getter
@AllArgsConstructor
public class CompiledRoute {

    private GatewayRoute gatewayRoute;
    private PathPattern pathPattern;
    private String sourcePathPrefix;
}

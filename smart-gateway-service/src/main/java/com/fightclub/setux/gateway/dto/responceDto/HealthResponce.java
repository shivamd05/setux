package com.fightclub.setux.gateway.dto.responceDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter

public class HealthResponce {

    private String serviceName ;
    private String serviceDescription ;
    private String appStatus ;
}

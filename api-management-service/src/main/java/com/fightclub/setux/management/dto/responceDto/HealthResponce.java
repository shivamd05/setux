package com.fightclub.setux.management.dto.responceDto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter

public class HealthResponce {

    private String serviceStatus ;
    private String dbStatus ;
    private LocalDateTime timeStamp ;


}
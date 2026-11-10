package com.fightclub.setux.gateway.dto.responceDto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class HeaderRule {

    private Long id;
    private String direction;
    private String action;
    private String headerName;
    private String headerValue;
}
package com.fightclub.setux.management.dto.responceDto;

import com.fightclub.setux.management.entity.RouteHeaderRule;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HeaderRuleResponse {

    private Long id;
    private String direction;
    private String action;
    private String headerName;
    private String headerValue;

    /** Converts a route header rule into its response DTO. */
    public static HeaderRuleResponse from(RouteHeaderRule rule) {
        return new HeaderRuleResponse(
                rule.getId(),
                rule.getDirection(),
                rule.getAction(),
                rule.getHeaderName(),
                rule.getHeaderValue()
        );
    }
}

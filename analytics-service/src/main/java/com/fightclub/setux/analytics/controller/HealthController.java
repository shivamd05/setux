package com.fightclub.setux.analytics.controller;

import com.fightclub.setux.analytics.dto.responceDto.HealthResponce;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")

public class HealthController {

    private String serviceName ;
    private String serviceDescription ;

    public HealthController(@Value("${info.app.name}") String serviceName ,
                            @Value("${info.app.description}") String serviceDescription
    ){
        this.serviceDescription = serviceDescription ;
        this.serviceName =serviceName ;
    }


    @GetMapping
    public ResponseEntity<HealthResponce> health (){

        return ResponseEntity.ok(new HealthResponce(serviceName,serviceDescription,"UP")) ;



    }


}

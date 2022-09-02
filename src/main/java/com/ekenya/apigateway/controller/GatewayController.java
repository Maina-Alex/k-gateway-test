package com.ekenya.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GatewayController {
    @GetMapping("/defaultFallback")
    public Map<String,String> defaultMessage(){
        Map<String,String> response=new LinkedHashMap<>();
        response.put("error", "too many requests");
        response.put("error description", "please wait");
        return response;
    }
}

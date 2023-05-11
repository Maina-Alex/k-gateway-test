package com.karaniDev.apigateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UniversalResponse {
    private Integer status;
    private String message;
    private Object data;
    private List<String> errors;
    private Integer totalItems;
}

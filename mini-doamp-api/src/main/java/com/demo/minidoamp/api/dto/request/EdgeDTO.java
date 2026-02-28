package com.demo.minidoamp.api.dto.request;

import lombok.Data;

@Data
public class EdgeDTO {
    private String sourceNodeCode;
    private String targetNodeCode;
    private String conditionExpr;
    private Integer sortOrder;
}

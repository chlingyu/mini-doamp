package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ThresholdDTO {

    @NotNull
    private Integer level;

    @NotNull
    private String compareType;

    private BigDecimal upperLimit;
    private BigDecimal lowerLimit;
}

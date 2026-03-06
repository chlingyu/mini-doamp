package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class WarnTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String statDate;
    private Long warnCount;
    private Integer maxWarnLevel;
}

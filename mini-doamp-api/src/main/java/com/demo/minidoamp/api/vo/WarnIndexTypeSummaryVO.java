package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class WarnIndexTypeSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String indexType;
    private Long indexCount;
    private String indexNames;
}

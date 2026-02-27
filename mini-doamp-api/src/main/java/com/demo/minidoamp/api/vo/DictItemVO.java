package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class DictItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String itemValue;
    private String itemLabel;
    private Integer sortOrder;
    private Integer status;
}

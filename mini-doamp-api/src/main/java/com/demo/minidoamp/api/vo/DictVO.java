package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DictVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String dictCode;
    private String dictName;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private List<DictItemVO> items;
}

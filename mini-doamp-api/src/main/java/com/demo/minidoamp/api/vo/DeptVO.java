package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeptVO {

    private Long id;
    private String deptName;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private List<DeptVO> children;
}

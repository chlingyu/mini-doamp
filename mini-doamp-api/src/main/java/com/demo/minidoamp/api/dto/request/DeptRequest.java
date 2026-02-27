package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DeptRequest {

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Long parentId = 0L;
    private Integer sortOrder = 0;
    private Integer status;
}

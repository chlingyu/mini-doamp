package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RoleRequest {

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private Integer status;
    private String remark;
}

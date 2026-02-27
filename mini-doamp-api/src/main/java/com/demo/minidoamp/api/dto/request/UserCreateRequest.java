package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private Long roleId;
    private Integer status;
}

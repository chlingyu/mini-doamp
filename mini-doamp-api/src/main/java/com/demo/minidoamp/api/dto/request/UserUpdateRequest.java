package com.demo.minidoamp.api.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String password;
    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private Long roleId;
    private Integer status;
}

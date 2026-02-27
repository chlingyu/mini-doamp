package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private String deptName;
    private Long roleId;
    private String roleName;
    private Integer status;
    private LocalDateTime createTime;
}

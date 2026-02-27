package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleVO {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}

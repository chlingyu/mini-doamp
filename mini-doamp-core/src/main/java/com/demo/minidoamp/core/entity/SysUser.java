package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Long deptId;
    private Long roleId;
    private Integer status;

    @TableLogic
    private Integer deleted;
}

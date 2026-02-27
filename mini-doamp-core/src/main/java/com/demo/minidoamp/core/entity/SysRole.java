package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_role")
public class SysRole extends BaseEntity {

    private String roleCode;
    private String roleName;
    private Integer status;
    private String remark;

    @TableLogic
    private Integer deleted;
}

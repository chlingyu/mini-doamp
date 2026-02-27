package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_dept")
public class SysDept extends BaseEntity {

    private String deptName;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;

    @TableLogic
    private Integer deleted;
}

package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sop_workflow")
public class SopWorkflow extends BaseEntity {

    private String workflowCode;
    private String workflowName;
    private Integer version;
    private Integer status;
    private String remark;
    private Long createBy;

    @TableLogic
    private Integer deleted;
}

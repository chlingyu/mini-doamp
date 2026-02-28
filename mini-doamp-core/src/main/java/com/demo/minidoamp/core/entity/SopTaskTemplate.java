package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sop_task_template")
public class SopTaskTemplate extends BaseEntity {

    private String templateName;
    private Long workflowId;
    private String contentParams;
    private String feedbackParams;
    private String triggerType;
    private String cronExpr;
    private Integer status;

    @TableLogic
    private Integer deleted;
}

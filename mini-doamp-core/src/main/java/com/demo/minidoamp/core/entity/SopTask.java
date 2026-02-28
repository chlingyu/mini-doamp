package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sop_task")
public class SopTask extends BaseEntity {

    private String taskCode;
    private String taskName;
    private Long templateId;
    private Long workflowId;
    private String status;
    private Long createBy;
    private LocalDateTime completeTime;

    @TableLogic
    private Integer deleted;
}

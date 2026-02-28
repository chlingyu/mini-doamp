package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskTemplateVO {

    private Long id;
    private String templateName;
    private Long workflowId;
    private String workflowName;
    private String contentParams;
    private String feedbackParams;
    private String triggerType;
    private String cronExpr;
    private Integer status;
    private LocalDateTime createTime;
}

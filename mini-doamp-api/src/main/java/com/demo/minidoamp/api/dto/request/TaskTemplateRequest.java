package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class TaskTemplateRequest {

    @NotBlank
    private String templateName;

    @NotNull
    private Long workflowId;

    private String contentParams;
    private String feedbackParams;
    private String triggerType;
    private String cronExpr;
    private Integer status;
}

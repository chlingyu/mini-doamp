package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class WarnRuleRequest {

    @NotBlank
    private String ruleName;

    @NotNull
    private Long indexId;

    private String notifyType;
    private String receiverIds;
    private String cronExpr;
    private Integer status;
}

package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AdvanceRequest {

    @NotBlank
    private String action;

    private String result;
    private String feedbackData;
    private String remark;
}

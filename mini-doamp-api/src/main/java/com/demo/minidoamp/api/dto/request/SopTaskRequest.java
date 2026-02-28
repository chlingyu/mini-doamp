package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class SopTaskRequest {

    @NotBlank
    private String taskName;

    @NotNull
    private Long templateId;
}

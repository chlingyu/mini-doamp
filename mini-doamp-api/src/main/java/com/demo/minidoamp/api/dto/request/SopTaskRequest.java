package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class SopTaskRequest {

    @NotBlank
    private String taskName;

    @NotNull
    private Long templateId;
}

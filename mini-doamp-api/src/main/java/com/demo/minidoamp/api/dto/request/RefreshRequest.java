package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RefreshRequest {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}

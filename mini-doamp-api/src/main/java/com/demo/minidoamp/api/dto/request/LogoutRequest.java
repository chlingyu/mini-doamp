package com.demo.minidoamp.api.dto.request;

import lombok.Data;

@Data
public class LogoutRequest {

    private String refreshToken;
}
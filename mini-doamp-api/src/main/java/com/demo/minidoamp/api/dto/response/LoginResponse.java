package com.demo.minidoamp.api.dto.response;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private String refreshToken;
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
}

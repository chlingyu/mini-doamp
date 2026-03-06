package com.demo.minidoamp.api.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {

    private String token;
    private String refreshToken;
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private List<String> permissions;
}
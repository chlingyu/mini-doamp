package com.demo.minidoamp.gateway.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.LoginRequest;
import com.demo.minidoamp.api.dto.request.RefreshRequest;
import com.demo.minidoamp.api.dto.response.LoginResponse;
import com.demo.minidoamp.gateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return R.ok(authService.refresh(req.getRefreshToken()));
    }
}

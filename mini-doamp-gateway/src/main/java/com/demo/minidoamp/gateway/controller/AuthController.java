package com.demo.minidoamp.gateway.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.LoginRequest;
import com.demo.minidoamp.api.dto.request.LogoutRequest;
import com.demo.minidoamp.api.dto.request.RefreshRequest;
import com.demo.minidoamp.api.dto.response.LoginResponse;
import com.demo.minidoamp.gateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

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

    @GetMapping("/userInfo")
    public R<Map<String, Object>> userInfo() {
        return R.ok(authService.currentUser(getCurrentUserId()));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestBody(required = false) LogoutRequest req,
                          HttpServletRequest request) {
        authService.logout(resolveBearerToken(request), req != null ? req.getRefreshToken() : null);
        return R.ok();
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
package com.demo.minidoamp.gateway.service;

import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.LoginRequest;
import com.demo.minidoamp.api.dto.response.LoginResponse;
import com.demo.minidoamp.core.entity.SysRole;
import com.demo.minidoamp.core.entity.SysUser;
import com.demo.minidoamp.core.mapper.SysRoleMapper;
import com.demo.minidoamp.core.mapper.SysUserMapper;
import com.demo.minidoamp.gateway.config.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest req) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername()));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setRefreshToken(refreshToken);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());

        if (user.getRoleId() != null) {
            SysRole role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                resp.setRoleCode(role.getRoleCode());
            }
        }
        return resp;
    }

    public LoginResponse refresh(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Claims claims = jwtUtil.parseToken(refreshToken);
        Long userId = claims.get("userId", Long.class);

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        String newToken = jwtUtil.generateToken(userId, user.getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, user.getUsername());

        LoginResponse resp = new LoginResponse();
        resp.setToken(newToken);
        resp.setRefreshToken(newRefreshToken);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        if (user.getRoleId() != null) {
            SysRole role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                resp.setRoleCode(role.getRoleCode());
            }
        }
        return resp;
    }
}

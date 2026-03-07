package com.demo.minidoamp.gateway.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.LoginRequest;
import com.demo.minidoamp.api.dto.response.LoginResponse;
import com.demo.minidoamp.core.entity.SysDept;
import com.demo.minidoamp.core.entity.SysRole;
import com.demo.minidoamp.core.entity.SysUser;
import com.demo.minidoamp.core.mapper.SysDeptMapper;
import com.demo.minidoamp.core.mapper.SysRoleMapper;
import com.demo.minidoamp.core.mapper.SysUserMapper;
import com.demo.minidoamp.gateway.config.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final PermissionService permissionService;

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
        return buildLoginResponse(user, token, refreshToken);
    }

    public LoginResponse refresh(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken)
                || !jwtUtil.isRefreshToken(refreshToken)
                || tokenBlacklistService.isBlacklisted(refreshToken)) {
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

        // 旧 refresh token 立即失效，防止重放攻击
        tokenBlacklistService.blacklist(refreshToken);

        return buildLoginResponse(user, newToken, newRefreshToken);
    }

    public Map<String, Object> currentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        SysRole role = user.getRoleId() != null ? roleMapper.selectById(user.getRoleId()) : null;
        SysDept dept = user.getDeptId() != null ? deptMapper.selectById(user.getDeptId()) : null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("roleCode", role != null ? role.getRoleCode() : null);
        result.put("roleName", role != null ? role.getRoleName() : null);
        result.put("deptName", dept != null ? dept.getDeptName() : null);
        result.put("permissions",
                permissionService.resolvePermissionsByRoleCode(role != null ? role.getRoleCode() : null));
        return result;
    }

    public void logout(String accessToken, String refreshToken) {
        tokenBlacklistService.blacklist(accessToken);
        tokenBlacklistService.blacklist(refreshToken);
    }

    private LoginResponse buildLoginResponse(SysUser user, String token, String refreshToken) {
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setRefreshToken(refreshToken);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());

        SysRole role = null;
        if (user.getRoleId() != null) {
            role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                resp.setRoleCode(role.getRoleCode());
            }
        }
        resp.setPermissions(permissionService.resolvePermissionsByRoleCode(role != null ? role.getRoleCode() : null));
        return resp;
    }
}

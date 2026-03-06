package com.demo.minidoamp.gateway.service;

import com.demo.minidoamp.core.entity.SysRole;
import com.demo.minidoamp.core.entity.SysUser;
import com.demo.minidoamp.core.mapper.SysRoleMapper;
import com.demo.minidoamp.core.mapper.SysUserMapper;
import com.demo.minidoamp.gateway.config.PermissionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PermissionProperties permissionProperties;

    public List<String> resolvePermissionsByUserId(Long userId) {
        if (userId == null) {
            return defaultPermissions();
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            return defaultPermissions();
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        return resolvePermissionsByRoleCode(role != null ? role.getRoleCode() : null);
    }

    public List<String> resolvePermissionsByRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return defaultPermissions();
        }
        String normalized = roleCode.toUpperCase();
        List<String> permissions = permissionProperties.getRoleMappings().get(normalized);
        if (permissions == null || permissions.isEmpty()) {
            return defaultPermissions();
        }
        return new ArrayList<String>(permissions);
    }

    private List<String> defaultPermissions() {
        List<String> permissions = permissionProperties.getDefaults();
        if (permissions == null || permissions.isEmpty()) {
            return Collections.singletonList("dashboard.view");
        }
        return new ArrayList<String>(permissions);
    }
}

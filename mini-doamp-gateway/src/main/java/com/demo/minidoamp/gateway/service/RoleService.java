package com.demo.minidoamp.gateway.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.RoleRequest;
import com.demo.minidoamp.api.vo.RoleVO;
import com.demo.minidoamp.core.entity.SysRole;
import com.demo.minidoamp.core.entity.SysUser;
import com.demo.minidoamp.core.mapper.SysRoleMapper;
import com.demo.minidoamp.core.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;

    public List<RoleVO> list() {
        List<SysRole> roles = roleMapper.selectList(null);
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public void create(RoleRequest req) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, req.getRoleCode()));
        if (count > 0) {
            throw new BusinessException("角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(req.getRoleCode());
        role.setRoleName(req.getRoleName());
        role.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        role.setRemark(req.getRemark());
        roleMapper.insert(role);
    }

    @Transactional
    public void update(Long id, RoleRequest req) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        role.setRoleName(req.getRoleName());
        role.setRemark(req.getRemark());
        if (req.getStatus() != null) {
            role.setStatus(req.getStatus());
        }
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, id));
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_HAS_USERS);
        }
        roleMapper.deleteById(id);
    }

    private RoleVO toVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setStatus(role.getStatus());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }
}

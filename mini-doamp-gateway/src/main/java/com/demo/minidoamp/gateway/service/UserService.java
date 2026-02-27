package com.demo.minidoamp.gateway.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.UserCreateRequest;
import com.demo.minidoamp.api.dto.request.UserUpdateRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.UserVO;
import com.demo.minidoamp.core.entity.SysDept;
import com.demo.minidoamp.core.entity.SysRole;
import com.demo.minidoamp.core.entity.SysUser;
import com.demo.minidoamp.core.mapper.SysDeptMapper;
import com.demo.minidoamp.core.mapper.SysRoleMapper;
import com.demo.minidoamp.core.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserVO> page(int pageNum, int pageSize, String keyword) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysUser::getUsername, keyword)
                   .or().like(SysUser::getRealName, keyword);
        }
        userMapper.selectPage(page, wrapper);
        List<UserVO> list = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    public UserVO getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    @Transactional
    public void create(UserCreateRequest req) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        validateDeptAndRole(req.getDeptId(), req.getRoleId());
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setDeptId(req.getDeptId());
        user.setRoleId(req.getRoleId());
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        userMapper.insert(user);
    }

    @Transactional
    public void update(Long id, UserUpdateRequest req) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        validateDeptAndRole(req.getDeptId(), req.getRoleId());
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setDeptId(req.getDeptId());
        user.setRoleId(req.getRoleId());
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        if (StringUtils.hasText(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        userMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    private void validateDeptAndRole(Long deptId, Long roleId) {
        if (deptId != null && deptMapper.selectById(deptId) == null) {
            throw new BusinessException(ErrorCode.DEPT_NOT_FOUND);
        }
        if (roleId != null && roleMapper.selectById(roleId) == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setDeptId(user.getDeptId());
        vo.setRoleId(user.getRoleId());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        if (user.getRoleId() != null) {
            SysRole role = roleMapper.selectById(user.getRoleId());
            if (role != null) vo.setRoleName(role.getRoleName());
        }
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null) vo.setDeptName(dept.getDeptName());
        }
        return vo;
    }
}

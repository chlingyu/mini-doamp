package com.demo.minidoamp.gateway.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.DeptRequest;
import com.demo.minidoamp.api.vo.DeptVO;
import com.demo.minidoamp.core.entity.SysDept;
import com.demo.minidoamp.core.entity.SysUser;
import com.demo.minidoamp.core.mapper.SysDeptMapper;
import com.demo.minidoamp.core.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    public List<DeptVO> tree() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .orderByAsc(SysDept::getSortOrder));
        List<DeptVO> voList = all.stream()
                .map(this::toVO).collect(Collectors.toList());
        return buildTree(voList, 0L);
    }

    @Transactional
    public void create(DeptRequest req) {
        SysDept dept = new SysDept();
        dept.setDeptName(req.getDeptName());
        dept.setParentId(req.getParentId());
        dept.setSortOrder(req.getSortOrder());
        dept.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        deptMapper.insert(dept);
    }

    @Transactional
    public void update(Long id, DeptRequest req) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        dept.setDeptName(req.getDeptName());
        dept.setParentId(req.getParentId());
        dept.setSortOrder(req.getSortOrder());
        if (req.getStatus() != null) {
            dept.setStatus(req.getStatus());
        }
        deptMapper.updateById(dept);
    }

    @Transactional
    public void delete(Long id) {
        Long childCount = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN);
        }
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_USERS);
        }
        deptMapper.deleteById(id);
    }

    private List<DeptVO> buildTree(List<DeptVO> list, Long parentId) {
        List<DeptVO> tree = new ArrayList<>();
        for (DeptVO vo : list) {
            if (parentId.equals(vo.getParentId())) {
                vo.setChildren(buildTree(list, vo.getId()));
                tree.add(vo);
            }
        }
        return tree;
    }

    private DeptVO toVO(SysDept dept) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setDeptName(dept.getDeptName());
        vo.setParentId(dept.getParentId());
        vo.setSortOrder(dept.getSortOrder());
        vo.setStatus(dept.getStatus());
        return vo;
    }
}

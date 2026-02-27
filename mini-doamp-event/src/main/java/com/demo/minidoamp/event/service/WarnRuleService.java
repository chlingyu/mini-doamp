package com.demo.minidoamp.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.WarnRuleRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.WarnRuleVO;
import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRule;
import com.demo.minidoamp.core.mapper.WarnIndexMapper;
import com.demo.minidoamp.core.mapper.WarnRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarnRuleService {

    private final WarnRuleMapper ruleMapper;
    private final WarnIndexMapper indexMapper;

    public PageResponse<WarnRuleVO> page(int pageNum, int pageSize, String keyword) {
        Page<WarnRule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WarnRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(WarnRule::getRuleName, keyword);
        }
        ruleMapper.selectPage(page, wrapper);
        List<WarnRuleVO> list = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    public WarnRuleVO getById(Long id) {
        WarnRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RULE_NOT_FOUND);
        }
        return toVO(rule);
    }

    @Transactional
    public void create(WarnRuleRequest req) {
        WarnIndex index = indexMapper.selectById(req.getIndexId());
        if (index == null) {
            throw new BusinessException(ErrorCode.INDEX_NOT_FOUND);
        }
        WarnRule rule = new WarnRule();
        rule.setRuleName(req.getRuleName());
        rule.setIndexId(req.getIndexId());
        rule.setNotifyType(req.getNotifyType());
        rule.setReceiverIds(req.getReceiverIds());
        rule.setCronExpr(req.getCronExpr());
        rule.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        ruleMapper.insert(rule);
    }

    @Transactional
    public void update(Long id, WarnRuleRequest req) {
        WarnRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RULE_NOT_FOUND);
        }
        WarnIndex index = indexMapper.selectById(req.getIndexId());
        if (index == null) {
            throw new BusinessException(ErrorCode.INDEX_NOT_FOUND);
        }
        rule.setRuleName(req.getRuleName());
        rule.setIndexId(req.getIndexId());
        rule.setNotifyType(req.getNotifyType());
        rule.setReceiverIds(req.getReceiverIds());
        rule.setCronExpr(req.getCronExpr());
        if (req.getStatus() != null) {
            rule.setStatus(req.getStatus());
        }
        ruleMapper.updateById(rule);
    }

    @Transactional
    public void delete(Long id) {
        ruleMapper.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        WarnRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.RULE_NOT_FOUND);
        }
        rule.setStatus(status);
        ruleMapper.updateById(rule);
    }

    private WarnRuleVO toVO(WarnRule rule) {
        WarnRuleVO vo = new WarnRuleVO();
        vo.setId(rule.getId());
        vo.setRuleName(rule.getRuleName());
        vo.setIndexId(rule.getIndexId());
        vo.setNotifyType(rule.getNotifyType());
        vo.setReceiverIds(rule.getReceiverIds());
        vo.setCronExpr(rule.getCronExpr());
        vo.setStatus(rule.getStatus());
        vo.setCreateTime(rule.getCreateTime());
        if (rule.getIndexId() != null) {
            WarnIndex index = indexMapper.selectById(rule.getIndexId());
            if (index != null) {
                vo.setIndexName(index.getIndexName());
                vo.setIndexType(index.getIndexType());
            }
        }
        return vo;
    }
}

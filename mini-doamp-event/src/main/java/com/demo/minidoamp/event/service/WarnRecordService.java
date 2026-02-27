package com.demo.minidoamp.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.WarnRecordVO;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.mapper.WarnRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarnRecordService {

    private final WarnRecordMapper recordMapper;

    public PageResponse<WarnRecordVO> page(int pageNum, int pageSize,
                                           Long ruleId, Long indexId) {
        Page<WarnRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WarnRecord> wrapper = new LambdaQueryWrapper<>();
        if (ruleId != null) {
            wrapper.eq(WarnRecord::getRuleId, ruleId);
        }
        if (indexId != null) {
            wrapper.eq(WarnRecord::getIndexId, indexId);
        }
        wrapper.orderByDesc(WarnRecord::getWarnTime);
        recordMapper.selectPage(page, wrapper);
        List<WarnRecordVO> list = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    private WarnRecordVO toVO(WarnRecord r) {
        WarnRecordVO vo = new WarnRecordVO();
        vo.setId(r.getId());
        vo.setRuleId(r.getRuleId());
        vo.setIndexId(r.getIndexId());
        vo.setIndexType(r.getIndexType());
        vo.setWarnLevel(r.getWarnLevel());
        vo.setCurrentValue(r.getCurrentValue());
        vo.setThresholdValue(r.getThresholdValue());
        vo.setGroupKey(r.getGroupKey());
        vo.setWarnTime(r.getWarnTime());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}

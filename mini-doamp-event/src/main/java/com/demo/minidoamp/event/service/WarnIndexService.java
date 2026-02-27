package com.demo.minidoamp.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.ThresholdDTO;
import com.demo.minidoamp.api.dto.request.WarnIndexRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.WarnIndexVO;
import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.CompareType;
import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.WarnIndexMapper;
import com.demo.minidoamp.core.mapper.WarnThresholdMapper;
import com.demo.minidoamp.event.util.CustomSqlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarnIndexService {

    private final WarnIndexMapper indexMapper;
    private final WarnThresholdMapper thresholdMapper;

    public PageResponse<WarnIndexVO> page(int pageNum, int pageSize, String keyword) {
        Page<WarnIndex> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WarnIndex> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(WarnIndex::getIndexName, keyword)
                   .or().like(WarnIndex::getIndexCode, keyword);
        }
        indexMapper.selectPage(page, wrapper);
        List<WarnIndexVO> list = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    public WarnIndexVO getById(Long id) {
        WarnIndex index = indexMapper.selectById(id);
        if (index == null) {
            throw new BusinessException(ErrorCode.INDEX_NOT_FOUND);
        }
        return toVO(index);
    }

    @Transactional
    public void create(WarnIndexRequest req) {
        Long count = indexMapper.selectCount(
                new LambdaQueryWrapper<WarnIndex>()
                        .eq(WarnIndex::getIndexCode, req.getIndexCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.INDEX_CODE_EXISTS);
        }
        validateRequest(req);

        WarnIndex index = new WarnIndex();
        index.setIndexCode(req.getIndexCode());
        index.setIndexName(req.getIndexName());
        index.setIndexType(req.getIndexType());
        index.setDataTable(req.getDataTable());
        index.setDataColumn(req.getDataColumn());
        index.setGroupColumn(req.getGroupColumn());
        index.setCustomSql(req.getCustomSql());
        index.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        index.setRemark(req.getRemark());
        indexMapper.insert(index);

        saveThresholds(index.getId(), req.getThresholds());
    }

    @Transactional
    public void update(Long id, WarnIndexRequest req) {
        WarnIndex index = indexMapper.selectById(id);
        if (index == null) {
            throw new BusinessException(ErrorCode.INDEX_NOT_FOUND);
        }
        // indexCode 唯一性校验（排除自身）
        Long count = indexMapper.selectCount(
                new LambdaQueryWrapper<WarnIndex>()
                        .eq(WarnIndex::getIndexCode, req.getIndexCode())
                        .ne(WarnIndex::getId, id));
        if (count > 0) {
            throw new BusinessException(ErrorCode.INDEX_CODE_EXISTS);
        }
        validateRequest(req);

        index.setIndexCode(req.getIndexCode());
        index.setIndexName(req.getIndexName());
        index.setIndexType(req.getIndexType());
        index.setDataTable(req.getDataTable());
        index.setDataColumn(req.getDataColumn());
        index.setGroupColumn(req.getGroupColumn());
        index.setCustomSql(req.getCustomSql());
        if (req.getStatus() != null) {
            index.setStatus(req.getStatus());
        }
        index.setRemark(req.getRemark());
        indexMapper.updateById(index);

        // 先删后插，重建阈值子表
        thresholdMapper.delete(
                new LambdaQueryWrapper<WarnThreshold>()
                        .eq(WarnThreshold::getIndexId, id));
        saveThresholds(id, req.getThresholds());
    }

    @Transactional
    public void delete(Long id) {
        indexMapper.deleteById(id);
        thresholdMapper.delete(
                new LambdaQueryWrapper<WarnThreshold>()
                        .eq(WarnThreshold::getIndexId, id));
    }

    private void saveThresholds(Long indexId, List<ThresholdDTO> dtos) {
        if (dtos == null) return;
        for (ThresholdDTO dto : dtos) {
            WarnThreshold t = new WarnThreshold();
            t.setIndexId(indexId);
            t.setLevel(dto.getLevel());
            t.setCompareType(dto.getCompareType());
            t.setUpperLimit(dto.getUpperLimit());
            t.setLowerLimit(dto.getLowerLimit());
            thresholdMapper.insert(t);
        }
    }

    private void validateRequest(WarnIndexRequest req) {
        // 校验 indexType 是合法枚举值
        if (!StringUtils.hasText(req.getIndexType())) {
            throw new BusinessException(ErrorCode.INVALID_INDEX_TYPE);
        }
        try {
            IndexType.valueOf(req.getIndexType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INDEX_TYPE);
        }

        // CUSTOM_SQL 类型必须有 SQL 内容
        if (IndexType.CUSTOM_SQL.name().equals(req.getIndexType())) {
            if (!StringUtils.hasText(req.getCustomSql())) {
                throw new BusinessException(ErrorCode.CUSTOM_SQL_INVALID);
            }
            CustomSqlValidator.validate(req.getCustomSql());
        }

        // 校验每个阈值的 compareType 和上下限必填规则
        if (req.getThresholds() != null) {
            for (ThresholdDTO t : req.getThresholds()) {
                if (!StringUtils.hasText(t.getCompareType())) {
                    throw new BusinessException(ErrorCode.INVALID_COMPARE_TYPE);
                }
                CompareType ct;
                try {
                    ct = CompareType.valueOf(t.getCompareType());
                } catch (IllegalArgumentException e) {
                    throw new BusinessException(ErrorCode.INVALID_COMPARE_TYPE);
                }
                switch (ct) {
                    case GT:
                    case GTE:
                    case EQ:
                        if (t.getUpperLimit() == null) {
                            throw new BusinessException(ErrorCode.THRESHOLD_LIMIT_REQUIRED);
                        }
                        break;
                    case LT:
                    case LTE:
                        if (t.getLowerLimit() == null) {
                            throw new BusinessException(ErrorCode.THRESHOLD_LIMIT_REQUIRED);
                        }
                        break;
                    case BETWEEN:
                        if (t.getUpperLimit() == null || t.getLowerLimit() == null) {
                            throw new BusinessException(ErrorCode.THRESHOLD_LIMIT_REQUIRED);
                        }
                        break;
                }
            }
        }
    }

    private WarnIndexVO toVO(WarnIndex index) {
        WarnIndexVO vo = new WarnIndexVO();
        vo.setId(index.getId());
        vo.setIndexCode(index.getIndexCode());
        vo.setIndexName(index.getIndexName());
        vo.setIndexType(index.getIndexType());
        vo.setDataTable(index.getDataTable());
        vo.setDataColumn(index.getDataColumn());
        vo.setGroupColumn(index.getGroupColumn());
        vo.setCustomSql(index.getCustomSql());
        vo.setStatus(index.getStatus());
        vo.setRemark(index.getRemark());
        vo.setCreateTime(index.getCreateTime());

        List<WarnThreshold> thresholds = thresholdMapper.selectList(
                new LambdaQueryWrapper<WarnThreshold>()
                        .eq(WarnThreshold::getIndexId, index.getId()));
        vo.setThresholds(thresholds.stream().map(t -> {
            WarnIndexVO.ThresholdVO tv = new WarnIndexVO.ThresholdVO();
            tv.setId(t.getId());
            tv.setLevel(t.getLevel());
            tv.setCompareType(t.getCompareType());
            tv.setUpperLimit(t.getUpperLimit());
            tv.setLowerLimit(t.getLowerLimit());
            return tv;
        }).collect(Collectors.toList()));
        return vo;
    }
}

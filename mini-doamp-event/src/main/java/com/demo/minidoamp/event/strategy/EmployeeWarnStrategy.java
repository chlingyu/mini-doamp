package com.demo.minidoamp.event.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.IndexGroup;
import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.IndexGroupMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 员工类预警策略。
 * <p>
 * 差异逻辑：只检查指标值超过最低有效阈值（floor=10）的员工数据，
 * 过滤掉小额/无效数据避免误报。同时跳过 groupName 为空的匿名记录。
 */
@Slf4j
@Component
public class EmployeeWarnStrategy extends AbstractWarnStrategy {

    /** 最低有效阈值：低于此值的员工指标不参与预警检查 */
    private static final BigDecimal FLOOR_VALUE = BigDecimal.TEN;

    private final IndexGroupMapper indexGroupMapper;

    public EmployeeWarnStrategy(IndexGroupMapper indexGroupMapper) {
        this.indexGroupMapper = indexGroupMapper;
    }

    @Override
    public String getType() {
        return IndexType.EMPLOYEE.getCode();
    }

    @Override
    public List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds) {
        // 取最新日期
        IndexGroup latest = indexGroupMapper.selectOne(
                new LambdaQueryWrapper<IndexGroup>()
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, "EMPLOYEE")
                        .orderByDesc(IndexGroup::getDataDate)
                        .last("LIMIT 1"));
        if (latest == null) {
            return Collections.emptyList();
        }
        LocalDate latestDate = latest.getDataDate();

        // 查该日期所有员工数据，过滤小额和匿名数据
        List<IndexGroup> groups = indexGroupMapper.selectList(
                new LambdaQueryWrapper<IndexGroup>()
                        .eq(IndexGroup::getIndexCode, index.getIndexCode())
                        .eq(IndexGroup::getGroupType, "EMPLOYEE")
                        .eq(IndexGroup::getDataDate, latestDate)
                        .isNotNull(IndexGroup::getGroupName)
                        .ge(IndexGroup::getIndexValue, FLOOR_VALUE));

        log.debug("员工预警检查: indexCode={}, date={}, 有效员工数={}",
                index.getIndexCode(), latestDate, groups.size());

        List<WarnRecord> records = new ArrayList<>();
        for (IndexGroup g : groups) {
            records.addAll(checkValue(index.getId(), index.getIndexType(),
                    g.getIndexValue(), g.getGroupKey(), thresholds));
        }
        return records;
    }
}

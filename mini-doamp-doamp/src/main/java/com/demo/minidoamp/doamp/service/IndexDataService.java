package com.demo.minidoamp.doamp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.IndexRunning;
import com.demo.minidoamp.core.mapper.IndexRunningMapper;
import com.demo.minidoamp.doamp.config.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexDataService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final IndexRunningMapper indexRunningMapper;
    private final CacheService cacheService;

    /**
     * 查询运行类指标最新值（带缓存）
     * Cache Aside 读链路：cache -> db -> 回填
     */
    public BigDecimal getRunningValue(String indexCode, LocalDate date) {
        String key = CacheConstants.INDEX_KEY_PREFIX + indexCode + ":" + date.format(DATE_FMT);

        // 1. 查缓存
        String cached = cacheService.get(key);
        if (cached != null) {
            if (cacheService.isNullValue(cached)) {
                return null;
            }
            return new BigDecimal(cached);
        }

        // 2. 查 DB
        IndexRunning record = indexRunningMapper.selectOne(
                new LambdaQueryWrapper<IndexRunning>()
                        .eq(IndexRunning::getIndexCode, indexCode)
                        .eq(IndexRunning::getDataDate, date)
                        .last("LIMIT 1"));

        // 3. 回填缓存
        if (record == null) {
            cacheService.setNull(key);
            return null;
        }

        cacheService.set(key, record.getIndexValue().toPlainString());
        return record.getIndexValue();
    }
}

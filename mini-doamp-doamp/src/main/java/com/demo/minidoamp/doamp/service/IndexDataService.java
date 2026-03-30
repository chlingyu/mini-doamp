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
     * 查询运行类指标最新值（带缓存 + 互斥回源防护）
     * 读链路：getOrLoad → cache miss → SETNX lock → db → 回填
     */
    public BigDecimal getRunningValue(String indexCode, LocalDate date) {
        String key = CacheConstants.INDEX_KEY_PREFIX + indexCode + ":" + date.format(DATE_FMT);

        String cached = cacheService.getOrLoad(key, () -> {
            // DB 回源
            IndexRunning record = indexRunningMapper.selectOne(
                    new LambdaQueryWrapper<IndexRunning>()
                            .eq(IndexRunning::getIndexCode, indexCode)
                            .eq(IndexRunning::getDataDate, date)
                            .last("LIMIT 1"));
            return record != null ? record.getIndexValue().toPlainString() : null;
        });

        return cached != null ? new BigDecimal(cached) : null;
    }
}

package com.demo.minidoamp.core.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 数据库方言演示 Mapper
 * 使用 databaseId 属性在 XML 中编写差异化 SQL
 */
@Mapper
public interface DemoDialectMapper {

    /**
     * 查询预警记录的格式化日期（演示日期函数差异）
     */
    List<Map<String, Object>> selectFormattedWarnDate(@Param("limit") int limit);

    /**
     * 查询指标分组聚合（演示 GROUP_CONCAT 差异）
     */
    List<Map<String, Object>> selectGroupedIndexNames();
}

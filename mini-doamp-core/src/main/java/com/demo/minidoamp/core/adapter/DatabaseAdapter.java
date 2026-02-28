package com.demo.minidoamp.core.adapter;

/**
 * 数据库方言适配器接口
 * 封装不同数据库的 SQL 语法差异
 */
public interface DatabaseAdapter {

    /**
     * 返回适配器对应的数据库标识（与 databaseIdProvider 一致）
     */
    String databaseId();

    /**
     * 日期格式化 SQL 片段
     *
     * @param column  列名
     * @param pattern 日期模式，统一使用 Java 风格（yyyy-MM-dd）
     * @return 数据库特定的日期格式化表达式
     */
    String dateFormat(String column, String pattern);

    /**
     * 分页 SQL 包装
     *
     * @param sql    原始 SQL
     * @param offset 偏移量（从 0 开始）
     * @param size   每页大小
     * @return 带分页的 SQL
     */
    String paginate(String sql, int offset, int size);

    /**
     * 字符串聚合函数
     *
     * @param column 列名
     * @return GROUP_CONCAT 表达式
     */
    String groupConcat(String column);
}

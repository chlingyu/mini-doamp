package com.demo.minidoamp.core.adapter;

import org.springframework.stereotype.Component;

/**
 * H2 方言适配器
 */
@Component
public class H2Adapter implements DatabaseAdapter {

    @Override
    public String databaseId() {
        return "h2";
    }

    @Override
    public String dateFormat(String column, String pattern) {
        // Java 风格直接兼容 H2 的 FORMATDATETIME
        return "FORMATDATETIME(" + column + ", '" + pattern + "')";
    }

    @Override
    public String paginate(String sql, int offset, int size) {
        return sql + " LIMIT " + size + " OFFSET " + offset;
    }

    @Override
    public String groupConcat(String column) {
        return "GROUP_CONCAT(" + column + ")";
    }
}

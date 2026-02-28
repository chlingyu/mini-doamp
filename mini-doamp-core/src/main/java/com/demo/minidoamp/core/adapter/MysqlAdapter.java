package com.demo.minidoamp.core.adapter;

import org.springframework.stereotype.Component;

/**
 * MySQL 方言适配器
 */
@Component
public class MysqlAdapter implements DatabaseAdapter {

    @Override
    public String databaseId() {
        return "mysql";
    }

    @Override
    public String dateFormat(String column, String pattern) {
        // Java 风格 → MySQL 风格：yyyy→%Y, MM→%m, dd→%d, HH→%H, mm→%i, ss→%s
        String mysqlPattern = pattern
                .replace("yyyy", "%Y")
                .replace("MM", "%m")
                .replace("dd", "%d")
                .replace("HH", "%H")
                .replace("mm", "%i")
                .replace("ss", "%s");
        return "DATE_FORMAT(" + column + ", '" + mysqlPattern + "')";
    }

    @Override
    public String paginate(String sql, int offset, int size) {
        return sql + " LIMIT " + offset + ", " + size;
    }

    @Override
    public String groupConcat(String column) {
        return "GROUP_CONCAT(" + column + ")";
    }
}

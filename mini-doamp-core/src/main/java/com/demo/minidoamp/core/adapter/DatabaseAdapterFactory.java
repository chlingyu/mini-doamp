package com.demo.minidoamp.core.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据库适配器工厂
 * 根据 databaseId 返回对应的 Adapter 实现
 */
@Slf4j
@Component
public class DatabaseAdapterFactory {

    private final Map<String, DatabaseAdapter> adapterMap;
    private final DatabaseAdapter defaultAdapter;

    public DatabaseAdapterFactory(List<DatabaseAdapter> adapters) {
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(DatabaseAdapter::databaseId, Function.identity()));
        this.defaultAdapter = adapterMap.get("mysql");
        log.info("DatabaseAdapterFactory initialized, available adapters: {}", adapterMap.keySet());
    }

    /**
     * 根据 databaseId 获取适配器
     *
     * @param databaseId 数据库标识（mysql / h2）
     * @return 对应的适配器，未匹配则返回 MySQL 适配器
     */
    public DatabaseAdapter getAdapter(String databaseId) {
        DatabaseAdapter adapter = adapterMap.get(databaseId);
        if (adapter == null) {
            log.warn("No adapter found for databaseId={}, fallback to mysql", databaseId);
            return defaultAdapter;
        }
        return adapter;
    }
}

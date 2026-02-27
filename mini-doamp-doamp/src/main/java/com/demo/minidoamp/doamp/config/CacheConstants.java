package com.demo.minidoamp.doamp.config;

public final class CacheConstants {

    private CacheConstants() {}

    // ========== key 前缀 ==========
    public static final String DICT_KEY_PREFIX = "dict:items:";
    public static final String INDEX_KEY_PREFIX = "index:data:";

    // ========== TTL（分钟） ==========
    public static final long BASE_TTL_MINUTES = 30;
    public static final long RANDOM_OFFSET_MINUTES = 5;
    public static final long NULL_TTL_MINUTES = 5;

    // ========== 空值标记 ==========
    public static final String NULL_VALUE = "__NULL__";
}

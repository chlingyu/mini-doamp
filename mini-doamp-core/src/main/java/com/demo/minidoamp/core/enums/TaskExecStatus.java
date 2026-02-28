package com.demo.minidoamp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum TaskExecStatus {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    DONE("DONE", "已完成"),
    REJECTED("REJECTED", "已驳回"),
    ROLLED_BACK("ROLLED_BACK", "已回退");

    private final String code;
    private final String desc;

    public static TaskExecStatus of(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TaskExecStatus: " + code));
    }
}

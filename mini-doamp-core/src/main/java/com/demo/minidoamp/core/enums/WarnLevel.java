package com.demo.minidoamp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WarnLevel {

    NORMAL(1, "一般"),
    IMPORTANT(2, "重要"),
    URGENT(3, "紧急");

    private final int code;
    private final String desc;
}

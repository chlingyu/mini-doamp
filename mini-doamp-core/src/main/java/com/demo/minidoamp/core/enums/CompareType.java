package com.demo.minidoamp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CompareType {

    GT("GT", "大于"),
    LT("LT", "小于"),
    GTE("GTE", "大于等于"),
    LTE("LTE", "小于等于"),
    EQ("EQ", "等于"),
    BETWEEN("BETWEEN", "区间");

    private final String code;
    private final String desc;
}

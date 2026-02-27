package com.demo.minidoamp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IndexType {

    RUNNING("RUNNING", "运行类"),
    OPERATION("OPERATION", "运营类"),
    BANK("BANK", "银行类"),
    CHANNEL("CHANNEL", "渠道效能类"),
    EMPLOYEE("EMPLOYEE", "员工类"),
    BRANCH("BRANCH", "营业部类"),
    CUSTOM_SQL("CUSTOM_SQL", "自定义SQL类");

    private final String code;
    private final String desc;
}

package com.demo.minidoamp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActionType {

    SUBMIT("SUBMIT", "提交"),
    APPROVE("APPROVE", "审批通过"),
    REJECT("REJECT", "驳回"),
    ROLLBACK("ROLLBACK", "回退"),
    TERMINATE("TERMINATE", "终止");

    private final String code;
    private final String desc;
}

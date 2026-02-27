package com.demo.minidoamp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatus {

    CREATED("CREATED", "已创建"),
    PENDING_ASSIGN("PENDING_ASSIGN", "待分配"),
    EXECUTING("EXECUTING", "执行中"),
    APPROVING("APPROVING", "审批中"),
    COMPLETED("COMPLETED", "已完成"),
    REJECTED("REJECTED", "已驳回"),
    TERMINATED("TERMINATED", "已终止");

    private final String code;
    private final String desc;
}

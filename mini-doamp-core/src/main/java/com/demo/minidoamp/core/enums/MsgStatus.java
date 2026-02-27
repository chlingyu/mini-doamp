package com.demo.minidoamp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MsgStatus {

    PENDING("PENDING", "待发送"),
    SENT("SENT", "已发送"),
    FAILED("FAILED", "发送失败"),
    RETRYING("RETRYING", "重试中"),
    ALARM("ALARM", "告警");

    private final String code;
    private final String desc;
}

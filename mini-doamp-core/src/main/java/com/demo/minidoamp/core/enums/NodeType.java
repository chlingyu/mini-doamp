package com.demo.minidoamp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NodeType {

    START("START", "开始节点"),
    PROCESS("PROCESS", "处理节点"),
    APPROVE("APPROVE", "审批节点"),
    COPY("COPY", "抄送节点"),
    BRANCH("BRANCH", "分支节点"),
    END("END", "结束节点");

    private final String code;
    private final String desc;
}

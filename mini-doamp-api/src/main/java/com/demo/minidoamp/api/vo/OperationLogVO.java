package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {

    private Long id;
    private Long taskId;
    private Long taskExecId;
    private Long nodeId;
    private String nodeName;
    private Long operatorId;
    private String operatorName;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String remark;
    private LocalDateTime createTime;
}

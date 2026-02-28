package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SopTaskVO {

    private Long id;
    private String taskCode;
    private String taskName;
    private Long templateId;
    private Long workflowId;
    private String workflowName;
    private String status;
    private String statusDesc;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
    private List<TaskExecVO> execRecords;
    private List<OperationLogVO> operationLogs;
}

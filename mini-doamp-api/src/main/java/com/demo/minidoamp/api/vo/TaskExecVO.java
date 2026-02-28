package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskExecVO {

    private Long id;
    private Long taskId;
    private Long nodeId;
    private String nodeName;
    private String nodeType;
    private Long assigneeId;
    private String assigneeName;
    private String status;
    private String result;
    private String feedbackData;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}

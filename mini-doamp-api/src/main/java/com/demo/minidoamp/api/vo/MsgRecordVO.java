package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MsgRecordVO {

    private Long id;
    private String msgId;
    private Long warnRecordId;
    private String notifyType;
    private Long receiverId;
    private String receiverName;
    private String receiverContact;
    private String title;
    private String content;
    private String status;
    private Integer retryCount;
    private String failReason;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
}

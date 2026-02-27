package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_msg_record")
public class MsgRecord implements Serializable {

    @TableId(type = IdType.AUTO)
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
    private LocalDateTime updateTime;
}

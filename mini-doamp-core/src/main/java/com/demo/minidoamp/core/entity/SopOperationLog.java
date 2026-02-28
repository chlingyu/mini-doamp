package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_sop_operation_log")
public class SopOperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long taskExecId;
    private Long nodeId;
    private Long operatorId;
    private String operatorName;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String remark;
    private LocalDateTime createTime;
}

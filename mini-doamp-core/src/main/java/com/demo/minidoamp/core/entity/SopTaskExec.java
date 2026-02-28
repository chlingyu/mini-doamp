package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_sop_task_exec")
public class SopTaskExec implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long nodeId;
    private Long assigneeId;
    private String status;
    private String result;
    private String feedbackData;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}

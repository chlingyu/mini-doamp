package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_job_exec_log")
public class JobExecLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobName;
    private String jobParam;
    private Integer status;
    private String message;
    private Long costMs;
    private LocalDateTime createTime;
}

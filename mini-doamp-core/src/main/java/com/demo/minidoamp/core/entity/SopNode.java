package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("t_sop_node")
public class SopNode implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private String nodeCode;
    private String nodeName;
    private String nodeType;
    private String assigneeType;
    private String assigneeId;
    private Integer sortOrder;
    private Integer xPos;
    private Integer yPos;
    private String properties;
}

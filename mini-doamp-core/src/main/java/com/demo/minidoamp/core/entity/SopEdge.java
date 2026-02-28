package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("t_sop_edge")
public class SopEdge implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workflowId;
    private Long sourceNodeId;
    private Long targetNodeId;
    private String conditionExpr;
    private Integer sortOrder;
}

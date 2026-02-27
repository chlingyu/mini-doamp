package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_index_operation")
public class IndexOperation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String indexCode;
    private BigDecimal indexValue;
    private LocalDate dataDate;
    private LocalDateTime createTime;
}

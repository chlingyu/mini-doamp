package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_warn_threshold")
public class WarnThreshold implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long indexId;
    private Integer level;
    private BigDecimal upperLimit;
    private BigDecimal lowerLimit;
    private String compareType;
    private LocalDateTime createTime;
}

package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_warn_record")
public class WarnRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ruleId;
    private Long indexId;
    private String indexType;
    private Integer warnLevel;
    private BigDecimal currentValue;
    private String thresholdValue;
    private String groupKey;
    private LocalDateTime warnTime;
    private LocalDateTime createTime;
}

package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WarnRecordVO {

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

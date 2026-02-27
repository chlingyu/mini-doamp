package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarnRuleVO {

    private Long id;
    private String ruleName;
    private Long indexId;
    private String indexName;
    private String indexType;
    private String notifyType;
    private String receiverIds;
    private String cronExpr;
    private Integer status;
    private LocalDateTime createTime;
}

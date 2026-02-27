package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WarnIndexVO {

    private Long id;
    private String indexCode;
    private String indexName;
    private String indexType;
    private String dataTable;
    private String dataColumn;
    private String groupColumn;
    private String customSql;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private List<ThresholdVO> thresholds;

    @Data
    public static class ThresholdVO {
        private Long id;
        private Integer level;
        private String compareType;
        private BigDecimal upperLimit;
        private BigDecimal lowerLimit;
    }
}

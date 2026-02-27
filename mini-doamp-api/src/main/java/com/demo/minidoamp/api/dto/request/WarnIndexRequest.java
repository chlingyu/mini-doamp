package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class WarnIndexRequest {

    @NotBlank
    private String indexCode;

    @NotBlank
    private String indexName;

    @NotBlank
    private String indexType;

    private String dataTable;
    private String dataColumn;
    private String groupColumn;
    private String customSql;
    private Integer status;
    private String remark;

    @Valid
    @NotNull
    private List<ThresholdDTO> thresholds;
}

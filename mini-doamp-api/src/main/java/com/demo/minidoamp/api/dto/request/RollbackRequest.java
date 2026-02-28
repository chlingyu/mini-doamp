package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class RollbackRequest {

    @NotNull
    private Long targetNodeId;

    private String remark;
}

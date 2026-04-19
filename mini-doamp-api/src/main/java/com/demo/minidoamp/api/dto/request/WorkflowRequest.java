package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class WorkflowRequest {

    @NotBlank
    private String workflowCode;

    @NotBlank
    private String workflowName;

    private String remark;

    @Valid
    @NotNull
    private List<NodeDTO> nodes;

    @Valid
    private List<EdgeDTO> edges;
}

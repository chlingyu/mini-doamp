package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class DictRequest {

    @NotBlank(message = "字典编码不能为空")
    private String dictCode;

    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    private Integer status;
    private String remark;

    @Valid
    private List<DictItemDTO> items;
}

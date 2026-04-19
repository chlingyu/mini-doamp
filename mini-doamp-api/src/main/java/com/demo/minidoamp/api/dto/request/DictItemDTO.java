package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class DictItemDTO {

    @NotBlank(message = "字典项值不能为空")
    private String itemValue;

    @NotBlank(message = "字典项标签不能为空")
    private String itemLabel;

    private Integer sortOrder;
    private Integer status;
}

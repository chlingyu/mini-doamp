package com.demo.minidoamp.api.dto.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class DictUpdateRequest {

    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    private Integer status;
    private String remark;

    /**
     * 全量替换字典项：传入则替换全部，不传(null)则不修改字典项
     */
    @Valid
    private List<DictItemDTO> items;
}

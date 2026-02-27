package com.demo.minidoamp.api.dto.request;

import lombok.Data;

@Data
public class PageRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
}

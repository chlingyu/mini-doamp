package com.demo.minidoamp.api.dto.response;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;
    private long total;
    private int pageNum;
    private int pageSize;

    public static <T> PageResponse<T> of(List<T> records, long total, int pageNum, int pageSize) {
        PageResponse<T> page = new PageResponse<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }
}

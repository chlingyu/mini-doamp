package com.demo.minidoamp.event.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.WarnRecordVO;
import com.demo.minidoamp.event.service.WarnRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warn/records")
@RequiredArgsConstructor
public class WarnRecordController {

    private final WarnRecordService warnRecordService;

    @GetMapping
    public R<PageResponse<WarnRecordVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) Long indexId) {
        return R.ok(warnRecordService.page(pageNum, pageSize, ruleId, indexId));
    }
}

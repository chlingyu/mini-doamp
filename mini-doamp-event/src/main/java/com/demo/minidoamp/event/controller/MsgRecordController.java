package com.demo.minidoamp.event.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.MsgRecordVO;
import com.demo.minidoamp.event.service.MsgRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/msg/records")
@RequiredArgsConstructor
public class MsgRecordController {

    private final MsgRecordService msgRecordService;

    @GetMapping
    public R<PageResponse<MsgRecordVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String notifyType) {
        return R.ok(msgRecordService.page(pageNum, pageSize, status, notifyType));
    }

    @PostMapping("/{id}/retry")
    public R<Void> retry(@PathVariable Long id) {
        msgRecordService.retry(id);
        return R.ok();
    }
}

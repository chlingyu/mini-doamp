package com.demo.minidoamp.event.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.WarnRuleRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.WarnRuleVO;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.event.engine.WarnEngine;
import com.demo.minidoamp.event.service.WarnRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/warn/rules")
@RequiredArgsConstructor
public class WarnRuleController {

    private final WarnRuleService warnRuleService;
    private final WarnEngine warnEngine;

    @GetMapping
    public R<PageResponse<WarnRuleVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(warnRuleService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public R<WarnRuleVO> getById(@PathVariable Long id) {
        return R.ok(warnRuleService.getById(id));
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody WarnRuleRequest req) {
        warnRuleService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody WarnRuleRequest req) {
        warnRuleService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        warnRuleService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id,
                                @RequestParam Integer status) {
        warnRuleService.updateStatus(id, status);
        return R.ok();
    }

    /** 手动触发预警检查 */
    @PostMapping("/{id}/trigger")
    public R<Integer> trigger(@PathVariable Long id) {
        List<WarnRecord> records = warnEngine.check(id);
        return R.ok(records.size());
    }
}

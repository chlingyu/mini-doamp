package com.demo.minidoamp.sop.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.WorkflowRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.WorkflowVO;
import com.demo.minidoamp.sop.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/sop/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    public R<PageResponse<WorkflowVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(workflowService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public R<WorkflowVO> getById(@PathVariable Long id) {
        return R.ok(workflowService.getById(id));
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody WorkflowRequest req) {
        workflowService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody WorkflowRequest req) {
        workflowService.update(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        workflowService.publish(id);
        return R.ok();
    }

    @PutMapping("/{id}/disable")
    public R<Void> disable(@PathVariable Long id) {
        workflowService.disable(id);
        return R.ok();
    }
}

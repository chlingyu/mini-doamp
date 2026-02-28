package com.demo.minidoamp.sop.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.TaskTemplateRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.TaskTemplateVO;
import com.demo.minidoamp.sop.service.TaskTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/sop/task-templates")
@RequiredArgsConstructor
public class TaskTemplateController {

    private final TaskTemplateService taskTemplateService;

    @GetMapping
    public R<PageResponse<TaskTemplateVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(taskTemplateService.page(pageNum, pageSize, keyword));
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody TaskTemplateRequest req) {
        taskTemplateService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TaskTemplateRequest req) {
        taskTemplateService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        taskTemplateService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        taskTemplateService.updateStatus(id, status);
        return R.ok();
    }
}

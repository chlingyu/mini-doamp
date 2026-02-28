package com.demo.minidoamp.sop.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.AdvanceRequest;
import com.demo.minidoamp.api.dto.request.RollbackRequest;
import com.demo.minidoamp.sop.engine.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/sop/task-execs")
@RequiredArgsConstructor
public class TaskExecController {

    private final WorkflowEngine workflowEngine;

    @PostMapping("/{id}/advance")
    public R<Void> advance(@PathVariable Long id,
                           @Valid @RequestBody AdvanceRequest req) {
        Long userId = getCurrentUserId();
        workflowEngine.advance(id, req.getAction(), req.getResult(),
                req.getFeedbackData(), req.getRemark(), userId);
        return R.ok();
    }

    @PostMapping("/{id}/rollback")
    public R<Void> rollback(@PathVariable Long id,
                            @Valid @RequestBody RollbackRequest req) {
        Long userId = getCurrentUserId();
        workflowEngine.rollback(id, req.getTargetNodeId(), req.getRemark(), userId);
        return R.ok();
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}

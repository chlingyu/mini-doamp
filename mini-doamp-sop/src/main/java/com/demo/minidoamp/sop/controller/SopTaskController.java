package com.demo.minidoamp.sop.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.SopTaskRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.SopTaskVO;
import com.demo.minidoamp.api.vo.TaskExecVO;
import com.demo.minidoamp.sop.service.SopTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/sop/tasks")
@RequiredArgsConstructor
public class SopTaskController {

    private final SopTaskService sopTaskService;

    @GetMapping
    public R<PageResponse<SopTaskVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(sopTaskService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public R<SopTaskVO> getById(@PathVariable Long id) {
        return R.ok(sopTaskService.getById(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody SopTaskRequest req) {
        Long userId = getCurrentUserId();
        return R.ok(sopTaskService.createTask(req, userId));
    }

    @PutMapping("/{id}/terminate")
    public R<Void> terminate(@PathVariable Long id) {
        sopTaskService.terminate(id, getCurrentUserId());
        return R.ok();
    }

    @GetMapping("/my-todo")
    public R<PageResponse<TaskExecVO>> myTodo(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(sopTaskService.myTodo(getCurrentUserId(), pageNum, pageSize));
    }

    @GetMapping("/my-done")
    public R<PageResponse<TaskExecVO>> myDone(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(sopTaskService.myDone(getCurrentUserId(), pageNum, pageSize));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}

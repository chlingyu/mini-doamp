package com.demo.minidoamp.gateway.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.DeptRequest;
import com.demo.minidoamp.api.vo.DeptVO;
import com.demo.minidoamp.gateway.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public R<List<DeptVO>> tree() {
        return R.ok(deptService.tree());
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody DeptRequest req) {
        deptService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody DeptRequest req) {
        deptService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }
}

package com.demo.minidoamp.gateway.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.RoleRequest;
import com.demo.minidoamp.api.vo.RoleVO;
import com.demo.minidoamp.gateway.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public R<List<RoleVO>> list() {
        return R.ok(roleService.list());
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody RoleRequest req) {
        roleService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody RoleRequest req) {
        roleService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }
}

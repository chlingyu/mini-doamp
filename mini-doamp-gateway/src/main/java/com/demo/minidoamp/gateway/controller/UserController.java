package com.demo.minidoamp.gateway.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.UserCreateRequest;
import com.demo.minidoamp.api.dto.request.UserUpdateRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.UserVO;
import com.demo.minidoamp.gateway.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public R<PageResponse<UserVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(userService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public R<UserVO> getById(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody UserCreateRequest req) {
        userService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody UserUpdateRequest req) {
        userService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }
}

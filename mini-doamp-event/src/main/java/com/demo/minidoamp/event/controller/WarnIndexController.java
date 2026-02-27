package com.demo.minidoamp.event.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.WarnIndexRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.WarnIndexVO;
import com.demo.minidoamp.event.service.WarnIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/warn/indexes")
@RequiredArgsConstructor
public class WarnIndexController {

    private final WarnIndexService warnIndexService;

    @GetMapping
    public R<PageResponse<WarnIndexVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(warnIndexService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public R<WarnIndexVO> getById(@PathVariable Long id) {
        return R.ok(warnIndexService.getById(id));
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody WarnIndexRequest req) {
        warnIndexService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @Valid @RequestBody WarnIndexRequest req) {
        warnIndexService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        warnIndexService.delete(id);
        return R.ok();
    }
}

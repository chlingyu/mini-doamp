package com.demo.minidoamp.doamp.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.request.DictRequest;
import com.demo.minidoamp.api.dto.request.DictUpdateRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.DictItemVO;
import com.demo.minidoamp.api.vo.DictVO;
import com.demo.minidoamp.doamp.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping
    public R<PageResponse<DictVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(dictService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public R<DictVO> getById(@PathVariable Long id) {
        return R.ok(dictService.getById(id));
    }

    @GetMapping("/items/{dictCode}")
    public R<List<DictItemVO>> getItemsByCode(@PathVariable String dictCode) {
        return R.ok(dictService.getItemsByCode(dictCode));
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody DictRequest req) {
        dictService.create(req);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody DictUpdateRequest req) {
        dictService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return R.ok();
    }
}

package com.demo.minidoamp.doamp.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.doamp.config.CacheConstants;
import com.demo.minidoamp.doamp.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache/refresh")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @PostMapping("/dict")
    public R<String> refreshAllDict() {
        int count = cacheService.deleteByPrefix(CacheConstants.DICT_KEY_PREFIX);
        return R.ok("已刷新字典缓存 " + count + " 个key");
    }

    @PostMapping("/dict/{dictCode}")
    public R<String> refreshDict(@PathVariable String dictCode) {
        cacheService.delete(CacheConstants.DICT_KEY_PREFIX + dictCode);
        return R.ok("已刷新字典缓存: " + dictCode);
    }

    @PostMapping("/index")
    public R<String> refreshAllIndex() {
        int count = cacheService.deleteByPrefix(CacheConstants.INDEX_KEY_PREFIX);
        return R.ok("已刷新指标缓存 " + count + " 个key");
    }

    @PostMapping("/all")
    public R<String> refreshAll() {
        int dictCount = cacheService.deleteByPrefix(CacheConstants.DICT_KEY_PREFIX);
        int indexCount = cacheService.deleteByPrefix(CacheConstants.INDEX_KEY_PREFIX);
        return R.ok("已刷新全部缓存: 字典" + dictCount + "个 + 指标" + indexCount + "个");
    }
}

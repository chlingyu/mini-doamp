package com.demo.minidoamp.doamp.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.DictItemDTO;
import com.demo.minidoamp.api.dto.request.DictRequest;
import com.demo.minidoamp.api.dto.request.DictUpdateRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.DictItemVO;
import com.demo.minidoamp.api.vo.DictVO;
import com.demo.minidoamp.core.entity.SysDict;
import com.demo.minidoamp.core.entity.SysDictItem;
import com.demo.minidoamp.core.mapper.SysDictItemMapper;
import com.demo.minidoamp.core.mapper.SysDictMapper;
import com.demo.minidoamp.doamp.config.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictMapper dictMapper;
    private final SysDictItemMapper dictItemMapper;
    private final CacheService cacheService;

    // ========== 查询（Cache Aside 读链路） ==========

    /**
     * 根据字典编码获取字典项列表（带缓存）
     * 读链路：cache -> db -> 回填
     */
    public List<DictItemVO> getItemsByCode(String dictCode) {
        String key = CacheConstants.DICT_KEY_PREFIX + dictCode;

        // 1. 查缓存
        String cached = cacheService.get(key);
        if (cached != null) {
            if (cacheService.isNullValue(cached)) {
                return Collections.emptyList();
            }
            return JSON.parseArray(cached, DictItemVO.class);
        }

        // 2. 查 DB
        SysDict dict = dictMapper.selectOne(
                new LambdaQueryWrapper<SysDict>().eq(SysDict::getDictCode, dictCode));
        if (dict == null) {
            // 防穿透：缓存空值
            cacheService.setNull(key);
            return Collections.emptyList();
        }

        List<SysDictItem> items = dictItemMapper.selectList(
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictId, dict.getId())
                        .eq(SysDictItem::getStatus, 1)
                        .orderByAsc(SysDictItem::getSortOrder));

        List<DictItemVO> voList = items.stream().map(this::toItemVO).collect(Collectors.toList());

        // 3. 回填缓存（随机 TTL 防雪崩）
        if (voList.isEmpty()) {
            cacheService.setNull(key);
        } else {
            cacheService.set(key, JSON.toJSONString(voList));
        }

        return voList;
    }

    // ========== 分页查询 ==========

    public PageResponse<DictVO> page(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysDict::getDictName, keyword)
                    .or().like(SysDict::getDictCode, keyword);
        }
        wrapper.orderByDesc(SysDict::getCreateTime);

        Page<SysDict> page = dictMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<DictVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResponse.of(voList, page.getTotal(), pageNum, pageSize);
    }

    // ========== 详情 ==========

    public DictVO getById(Long id) {
        SysDict dict = dictMapper.selectById(id);
        if (dict == null) {
            throw new BusinessException(ErrorCode.DICT_NOT_FOUND);
        }
        return toVO(dict);
    }

    // ========== 创建 ==========

    @Transactional
    public void create(DictRequest req) {
        // 校验编码唯一
        SysDict existing = dictMapper.selectOne(
                new LambdaQueryWrapper<SysDict>().eq(SysDict::getDictCode, req.getDictCode()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.DICT_CODE_EXISTS);
        }

        SysDict dict = new SysDict();
        dict.setDictCode(req.getDictCode());
        dict.setDictName(req.getDictName());
        dict.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        dict.setRemark(req.getRemark());
        dictMapper.insert(dict);

        saveItems(dict.getId(), req.getItems());

        // 事务提交后删除可能存在的空值缓存
        registerAfterCommitDelete(CacheConstants.DICT_KEY_PREFIX + dict.getDictCode());
    }

    // ========== 更新（延迟双删） ==========

    @Transactional
    public void update(Long id, DictUpdateRequest req) {
        SysDict dict = dictMapper.selectById(id);
        if (dict == null) {
            throw new BusinessException(ErrorCode.DICT_NOT_FOUND);
        }

        dict.setDictName(req.getDictName());
        dict.setStatus(req.getStatus() != null ? req.getStatus() : dict.getStatus());
        dict.setRemark(req.getRemark());
        dictMapper.updateById(dict);

        // items 非 null 时全量替换字典项，null 时不修改
        if (req.getItems() != null) {
            dictItemMapper.delete(
                    new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictId, id));
            saveItems(id, req.getItems());
        }

        // 事务提交后执行延迟双删
        String key = CacheConstants.DICT_KEY_PREFIX + dict.getDictCode();
        registerAfterCommitDoubleDelete(key);
    }

    // ========== 删除 ==========

    @Transactional
    public void delete(Long id) {
        SysDict dict = dictMapper.selectById(id);
        if (dict == null) {
            throw new BusinessException(ErrorCode.DICT_NOT_FOUND);
        }

        dictMapper.deleteById(id);
        dictItemMapper.delete(
                new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictId, id));

        // 事务提交后删除缓存
        registerAfterCommitDelete(CacheConstants.DICT_KEY_PREFIX + dict.getDictCode());
    }

    // ========== 事务提交后缓存操作 ==========

    /**
     * 事务提交后执行延迟双删
     * 严格顺序：DB 提交 → 删缓存 → 延迟 500ms → 再删缓存
     */
    private void registerAfterCommitDoubleDelete(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheService.doubleDelete(key);
            }
        });
    }

    /**
     * 事务提交后直接删除缓存
     */
    private void registerAfterCommitDelete(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheService.delete(key);
            }
        });
    }

    // ========== 私有方法 ==========

    private void saveItems(Long dictId, List<DictItemDTO> items) {
        if (items == null || items.isEmpty()) return;
        for (int i = 0; i < items.size(); i++) {
            DictItemDTO dto = items.get(i);
            SysDictItem item = new SysDictItem();
            item.setDictId(dictId);
            item.setItemValue(dto.getItemValue());
            item.setItemLabel(dto.getItemLabel());
            item.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : i);
            item.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
            item.setCreateTime(LocalDateTime.now());
            dictItemMapper.insert(item);
        }
    }

    private DictVO toVO(SysDict dict) {
        DictVO vo = new DictVO();
        vo.setId(dict.getId());
        vo.setDictCode(dict.getDictCode());
        vo.setDictName(dict.getDictName());
        vo.setStatus(dict.getStatus());
        vo.setRemark(dict.getRemark());
        vo.setCreateTime(dict.getCreateTime());

        List<SysDictItem> items = dictItemMapper.selectList(
                new LambdaQueryWrapper<SysDictItem>()
                        .eq(SysDictItem::getDictId, dict.getId())
                        .orderByAsc(SysDictItem::getSortOrder));
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    private DictItemVO toItemVO(SysDictItem item) {
        DictItemVO vo = new DictItemVO();
        vo.setId(item.getId());
        vo.setItemValue(item.getItemValue());
        vo.setItemLabel(item.getItemLabel());
        vo.setSortOrder(item.getSortOrder());
        vo.setStatus(item.getStatus());
        return vo;
    }
}

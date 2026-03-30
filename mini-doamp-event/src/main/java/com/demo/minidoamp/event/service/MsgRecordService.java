package com.demo.minidoamp.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.MsgRecordVO;
import com.demo.minidoamp.core.entity.MsgRecord;
import com.demo.minidoamp.core.enums.MsgStatus;
import com.demo.minidoamp.core.mapper.MsgRecordMapper;
import com.demo.minidoamp.event.mq.producer.WarnMessageProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MsgRecordService {

    private final MsgRecordMapper msgRecordMapper;
    private final WarnMessageProducer messageProducer;

    public PageResponse<MsgRecordVO> page(int pageNum, int pageSize,
                                          String status, String notifyType) {
        Page<MsgRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MsgRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(MsgRecord::getStatus, status);
        }
        if (StringUtils.hasText(notifyType)) {
            wrapper.eq(MsgRecord::getNotifyType, notifyType);
        }
        wrapper.orderByDesc(MsgRecord::getCreateTime);
        msgRecordMapper.selectPage(page, wrapper);
        List<MsgRecordVO> list = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    private static final int MAX_RETRY = 3;

    /**
     * 手动重试消息发送。
     * <p>使用条件更新（CAS）保证并发安全：
     * WHERE id=? AND status='FAILED' AND retry_count < MAX_RETRY，
     * 只有一个线程能成功更新，其余因 affected=0 而被拒绝。</p>
     */
    public void retry(Long id) {
        MsgRecord record = msgRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 只允许 FAILED 状态重试（ALARM 是终态，需人工处理）
        if (!MsgStatus.FAILED.getCode().equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.MSG_ALREADY_SENT);
        }
        // 已达最大重试次数：CAS 置为 ALARM
        if (record.getRetryCount() >= MAX_RETRY) {
            int alarmed = msgRecordMapper.update(null, new LambdaUpdateWrapper<MsgRecord>()
                    .eq(MsgRecord::getId, id)
                    .eq(MsgRecord::getStatus, MsgStatus.FAILED.getCode())
                    .set(MsgRecord::getStatus, MsgStatus.ALARM.getCode())
                    .set(MsgRecord::getUpdateTime, LocalDateTime.now()));
            if (alarmed == 0) {
                // 并发已被其他线程处理
                throw new BusinessException(ErrorCode.MSG_ALREADY_SENT);
            }
            throw new BusinessException(ErrorCode.MSG_RETRY_EXCEEDED);
        }

        // CAS 条件更新：status=FAILED AND retry_count < MAX_RETRY → RETRYING
        int updated = msgRecordMapper.update(null, new LambdaUpdateWrapper<MsgRecord>()
                .eq(MsgRecord::getId, id)
                .eq(MsgRecord::getStatus, MsgStatus.FAILED.getCode())
                .lt(MsgRecord::getRetryCount, MAX_RETRY)
                .set(MsgRecord::getStatus, MsgStatus.RETRYING.getCode())
                .set(MsgRecord::getRetryCount, record.getRetryCount() + 1)
                .set(MsgRecord::getUpdateTime, LocalDateTime.now()));
        if (updated == 0) {
            // 并发已被其他线程抢先重试
            throw new BusinessException(ErrorCode.MSG_ALREADY_SENT);
        }

        messageProducer.republish(record);
    }

    private MsgRecordVO toVO(MsgRecord r) {
        MsgRecordVO vo = new MsgRecordVO();
        vo.setId(r.getId());
        vo.setMsgId(r.getMsgId());
        vo.setWarnRecordId(r.getWarnRecordId());
        vo.setNotifyType(r.getNotifyType());
        vo.setReceiverId(r.getReceiverId());
        vo.setReceiverName(r.getReceiverName());
        vo.setReceiverContact(r.getReceiverContact());
        vo.setTitle(r.getTitle());
        vo.setContent(r.getContent());
        vo.setStatus(r.getStatus());
        vo.setRetryCount(r.getRetryCount());
        vo.setFailReason(r.getFailReason());
        vo.setSendTime(r.getSendTime());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}

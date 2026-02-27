package com.demo.minidoamp.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public void retry(Long id) {
        MsgRecord record = msgRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String st = record.getStatus();
        if (!MsgStatus.FAILED.getCode().equals(st) && !MsgStatus.ALARM.getCode().equals(st)) {
            throw new BusinessException(ErrorCode.MSG_ALREADY_SENT);
        }
        record.setStatus(MsgStatus.RETRYING.getCode());
        record.setRetryCount(record.getRetryCount() + 1);
        record.setUpdateTime(LocalDateTime.now());
        msgRecordMapper.updateById(record);

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

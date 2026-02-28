package com.demo.minidoamp.sop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.TaskTemplateRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.TaskTemplateVO;
import com.demo.minidoamp.core.entity.SopTaskTemplate;
import com.demo.minidoamp.core.entity.SopWorkflow;
import com.demo.minidoamp.core.mapper.SopTaskTemplateMapper;
import com.demo.minidoamp.core.mapper.SopWorkflowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskTemplateService {

    private final SopTaskTemplateMapper templateMapper;
    private final SopWorkflowMapper workflowMapper;

    public PageResponse<TaskTemplateVO> page(int pageNum, int pageSize, String keyword) {
        Page<SopTaskTemplate> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SopTaskTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SopTaskTemplate::getTemplateName, keyword);
        }
        templateMapper.selectPage(page, wrapper);
        List<TaskTemplateVO> list = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    @Transactional
    public void create(TaskTemplateRequest req) {
        SopWorkflow wf = workflowMapper.selectById(req.getWorkflowId());
        if (wf == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        SopTaskTemplate tpl = new SopTaskTemplate();
        tpl.setTemplateName(req.getTemplateName());
        tpl.setWorkflowId(req.getWorkflowId());
        tpl.setContentParams(req.getContentParams());
        tpl.setFeedbackParams(req.getFeedbackParams());
        tpl.setTriggerType(req.getTriggerType() != null ? req.getTriggerType() : "MANUAL");
        tpl.setCronExpr(req.getCronExpr());
        tpl.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        templateMapper.insert(tpl);
    }

    @Transactional
    public void update(Long id, TaskTemplateRequest req) {
        SopTaskTemplate tpl = templateMapper.selectById(id);
        if (tpl == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        tpl.setTemplateName(req.getTemplateName());
        tpl.setWorkflowId(req.getWorkflowId());
        tpl.setContentParams(req.getContentParams());
        tpl.setFeedbackParams(req.getFeedbackParams());
        tpl.setTriggerType(req.getTriggerType());
        tpl.setCronExpr(req.getCronExpr());
        if (req.getStatus() != null) {
            tpl.setStatus(req.getStatus());
        }
        templateMapper.updateById(tpl);
    }

    @Transactional
    public void delete(Long id) {
        templateMapper.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        SopTaskTemplate tpl = templateMapper.selectById(id);
        if (tpl == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        tpl.setStatus(status);
        templateMapper.updateById(tpl);
    }

    private TaskTemplateVO toVO(SopTaskTemplate tpl) {
        TaskTemplateVO vo = new TaskTemplateVO();
        vo.setId(tpl.getId());
        vo.setTemplateName(tpl.getTemplateName());
        vo.setWorkflowId(tpl.getWorkflowId());
        vo.setContentParams(tpl.getContentParams());
        vo.setFeedbackParams(tpl.getFeedbackParams());
        vo.setTriggerType(tpl.getTriggerType());
        vo.setCronExpr(tpl.getCronExpr());
        vo.setStatus(tpl.getStatus());
        vo.setCreateTime(tpl.getCreateTime());
        SopWorkflow wf = workflowMapper.selectById(tpl.getWorkflowId());
        if (wf != null) {
            vo.setWorkflowName(wf.getWorkflowName());
        }
        return vo;
    }
}

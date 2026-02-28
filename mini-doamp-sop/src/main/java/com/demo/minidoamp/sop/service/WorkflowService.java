package com.demo.minidoamp.sop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.EdgeDTO;
import com.demo.minidoamp.api.dto.request.NodeDTO;
import com.demo.minidoamp.api.dto.request.WorkflowRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.WorkflowVO;
import com.demo.minidoamp.core.entity.SopEdge;
import com.demo.minidoamp.core.entity.SopNode;
import com.demo.minidoamp.core.entity.SopWorkflow;
import com.demo.minidoamp.core.mapper.SopEdgeMapper;
import com.demo.minidoamp.core.mapper.SopNodeMapper;
import com.demo.minidoamp.core.mapper.SopWorkflowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final SopWorkflowMapper workflowMapper;
    private final SopNodeMapper nodeMapper;
    private final SopEdgeMapper edgeMapper;

    public PageResponse<WorkflowVO> page(int pageNum, int pageSize, String keyword) {
        Page<SopWorkflow> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SopWorkflow> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SopWorkflow::getWorkflowName, keyword);
        }
        wrapper.orderByDesc(SopWorkflow::getCreateTime);
        workflowMapper.selectPage(page, wrapper);
        List<WorkflowVO> list = page.getRecords().stream()
                .map(this::toSimpleVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    public WorkflowVO getById(Long id) {
        SopWorkflow wf = workflowMapper.selectById(id);
        if (wf == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        return toDetailVO(wf);
    }

    @Transactional
    public void create(WorkflowRequest req) {
        // 校验编码唯一
        Long count = workflowMapper.selectCount(
                new LambdaQueryWrapper<SopWorkflow>()
                        .eq(SopWorkflow::getWorkflowCode, req.getWorkflowCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.WORKFLOW_CODE_EXISTS);
        }
        SopWorkflow wf = new SopWorkflow();
        wf.setWorkflowCode(req.getWorkflowCode());
        wf.setWorkflowName(req.getWorkflowName());
        wf.setRemark(req.getRemark());
        wf.setStatus(0); // draft
        wf.setVersion(1);
        workflowMapper.insert(wf);
        saveNodesAndEdges(wf.getId(), req);
    }

    @Transactional
    public void update(Long id, WorkflowRequest req) {
        SopWorkflow wf = workflowMapper.selectById(id);
        if (wf == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        wf.setWorkflowName(req.getWorkflowName());
        wf.setRemark(req.getRemark());
        workflowMapper.updateById(wf);
        // 删除旧节点和连线，重新保存
        nodeMapper.delete(new LambdaQueryWrapper<SopNode>().eq(SopNode::getWorkflowId, id));
        edgeMapper.delete(new LambdaQueryWrapper<SopEdge>().eq(SopEdge::getWorkflowId, id));
        saveNodesAndEdges(id, req);
    }

    @Transactional
    public void publish(Long id) {
        SopWorkflow wf = workflowMapper.selectById(id);
        if (wf == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        wf.setStatus(1);
        workflowMapper.updateById(wf);
    }

    @Transactional
    public void disable(Long id) {
        SopWorkflow wf = workflowMapper.selectById(id);
        if (wf == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        wf.setStatus(2);
        workflowMapper.updateById(wf);
    }

    /** 级联保存节点+连线，API用nodeCode，插入后映射为nodeId */
    private void saveNodesAndEdges(Long workflowId, WorkflowRequest req) {
        Map<String, Long> codeToId = new HashMap<>();
        for (NodeDTO dto : req.getNodes()) {
            SopNode node = new SopNode();
            node.setWorkflowId(workflowId);
            node.setNodeCode(dto.getNodeCode());
            node.setNodeName(dto.getNodeName());
            node.setNodeType(dto.getNodeType());
            node.setAssigneeType(dto.getAssigneeType());
            node.setAssigneeId(dto.getAssigneeId());
            node.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
            node.setXPos(dto.getXPos());
            node.setYPos(dto.getYPos());
            node.setProperties(dto.getProperties());
            nodeMapper.insert(node);
            codeToId.put(dto.getNodeCode(), node.getId());
        }
        if (req.getEdges() != null) {
            for (EdgeDTO dto : req.getEdges()) {
                SopEdge edge = new SopEdge();
                edge.setWorkflowId(workflowId);
                edge.setSourceNodeId(codeToId.get(dto.getSourceNodeCode()));
                edge.setTargetNodeId(codeToId.get(dto.getTargetNodeCode()));
                edge.setConditionExpr(dto.getConditionExpr());
                edge.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
                edgeMapper.insert(edge);
            }
        }
    }

    private WorkflowVO toSimpleVO(SopWorkflow wf) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(wf.getId());
        vo.setWorkflowCode(wf.getWorkflowCode());
        vo.setWorkflowName(wf.getWorkflowName());
        vo.setVersion(wf.getVersion());
        vo.setStatus(wf.getStatus());
        vo.setRemark(wf.getRemark());
        vo.setCreateTime(wf.getCreateTime());
        return vo;
    }

    private WorkflowVO toDetailVO(SopWorkflow wf) {
        WorkflowVO vo = toSimpleVO(wf);
        List<SopNode> nodes = nodeMapper.selectList(
                new LambdaQueryWrapper<SopNode>()
                        .eq(SopNode::getWorkflowId, wf.getId())
                        .orderByAsc(SopNode::getSortOrder));
        vo.setNodes(nodes.stream().map(n -> {
            WorkflowVO.NodeVO nv = new WorkflowVO.NodeVO();
            nv.setId(n.getId());
            nv.setNodeCode(n.getNodeCode());
            nv.setNodeName(n.getNodeName());
            nv.setNodeType(n.getNodeType());
            nv.setAssigneeType(n.getAssigneeType());
            nv.setAssigneeId(n.getAssigneeId());
            nv.setSortOrder(n.getSortOrder());
            nv.setXPos(n.getXPos());
            nv.setYPos(n.getYPos());
            nv.setProperties(n.getProperties());
            return nv;
        }).collect(Collectors.toList()));
        List<SopEdge> edges = edgeMapper.selectList(
                new LambdaQueryWrapper<SopEdge>()
                        .eq(SopEdge::getWorkflowId, wf.getId()));
        vo.setEdges(edges.stream().map(e -> {
            WorkflowVO.EdgeVO ev = new WorkflowVO.EdgeVO();
            ev.setId(e.getId());
            ev.setSourceNodeId(e.getSourceNodeId());
            ev.setTargetNodeId(e.getTargetNodeId());
            ev.setConditionExpr(e.getConditionExpr());
            ev.setSortOrder(e.getSortOrder());
            return ev;
        }).collect(Collectors.toList()));
        return vo;
    }
}

package com.demo.minidoamp.sop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.dto.request.SopTaskRequest;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.vo.OperationLogVO;
import com.demo.minidoamp.api.vo.SopTaskVO;
import com.demo.minidoamp.api.vo.TaskExecVO;
import com.demo.minidoamp.core.entity.*;
import com.demo.minidoamp.core.enums.TaskExecStatus;
import com.demo.minidoamp.core.enums.TaskStatus;
import com.demo.minidoamp.core.mapper.*;
import com.demo.minidoamp.sop.engine.SopNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SopTaskService {

    private final SopTaskMapper taskMapper;
    private final SopTaskExecMapper taskExecMapper;
    private final SopTaskTemplateMapper templateMapper;
    private final SopWorkflowMapper workflowMapper;
    private final SopNodeMapper nodeMapper;
    private final SopEdgeMapper edgeMapper;
    private final SopOperationLogMapper operationLogMapper;
    private final SysUserMapper userMapper;
    private final SopNotifier sopNotifier;

    public PageResponse<SopTaskVO> page(int pageNum, int pageSize, String keyword) {
        Page<SopTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SopTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SopTask::getTaskName, keyword);
        }
        wrapper.orderByDesc(SopTask::getCreateTime);
        taskMapper.selectPage(page, wrapper);
        List<SopTaskVO> list = page.getRecords().stream()
                .map(this::toSimpleVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    public SopTaskVO getById(Long id) {
        SopTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        return toDetailVO(task);
    }

    /** 手动创建任务 */
    @Transactional
    public Long createTask(SopTaskRequest req, Long operatorId) {
        SopTaskTemplate tpl = templateMapper.selectById(req.getTemplateId());
        if (tpl == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        SopWorkflow wf = workflowMapper.selectById(tpl.getWorkflowId());
        if (wf == null || wf.getStatus() != 1) {
            throw new BusinessException(ErrorCode.WORKFLOW_NOT_PUBLISHED);
        }
        return doCreateTask(req.getTaskName(), tpl, wf, operatorId);
    }

    /** 定时任务调用的创建方法 */
    @Transactional
    public Long createTaskByTemplate(SopTaskTemplate tpl) {
        SopWorkflow wf = workflowMapper.selectById(tpl.getWorkflowId());
        if (wf == null || wf.getStatus() != 1) {
            return null;
        }
        String taskName = tpl.getTemplateName() + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return doCreateTask(taskName, tpl, wf, null);
    }

    private Long doCreateTask(String taskName, SopTaskTemplate tpl, SopWorkflow wf, Long createBy) {
        SopTask task = new SopTask();
        task.setTaskCode("TASK_" + System.currentTimeMillis());
        task.setTaskName(taskName);
        task.setTemplateId(tpl.getId());
        task.setWorkflowId(wf.getId());
        task.setStatus(TaskStatus.CREATED.getCode());
        task.setCreateBy(createBy);
        taskMapper.insert(task);
        writeLog(task.getId(), null, null, createBy, "CREATE",
                null, TaskStatus.CREATED.getCode(), null);

        // 找到START节点后的第一个执行节点，创建执行记录
        SopNode startNode = nodeMapper.selectOne(
                new LambdaQueryWrapper<SopNode>()
                        .eq(SopNode::getWorkflowId, wf.getId())
                        .eq(SopNode::getNodeType, "START"));
        if (startNode != null) {
            SopNode firstNode = findNextNode(wf.getId(), startNode.getId());
            if (firstNode != null) {
                // CREATED → PENDING_ASSIGN
                task.setStatus(TaskStatus.PENDING_ASSIGN.getCode());
                taskMapper.updateById(task);
                writeLog(task.getId(), null, firstNode.getId(), createBy, "ASSIGN",
                        TaskStatus.CREATED.getCode(), TaskStatus.PENDING_ASSIGN.getCode(), null);

                // PENDING_ASSIGN → EXECUTING
                createExecRecord(task.getId(), firstNode);
                task.setStatus(TaskStatus.EXECUTING.getCode());
                taskMapper.updateById(task);
                writeLog(task.getId(), null, firstNode.getId(), createBy, "START_EXEC",
                        TaskStatus.PENDING_ASSIGN.getCode(), TaskStatus.EXECUTING.getCode(), null);
            }
        }
        return task.getId();
    }

    /** 查找下一个节点（默认取第一条边） */
    public SopNode findNextNode(Long workflowId, Long currentNodeId) {
        SopEdge edge = edgeMapper.selectOne(
                new LambdaQueryWrapper<SopEdge>()
                        .eq(SopEdge::getWorkflowId, workflowId)
                        .eq(SopEdge::getSourceNodeId, currentNodeId)
                        .orderByAsc(SopEdge::getSortOrder)
                        .last("LIMIT 1"));
        if (edge == null) {
            return null;
        }
        return nodeMapper.selectById(edge.getTargetNodeId());
    }

    /** 分支节点按 conditionExpr 匹配 result 路由，无匹配则走默认（最后一条边） */
    public SopNode findNextNodeByCondition(Long workflowId, Long currentNodeId, String result) {
        List<SopEdge> edges = edgeMapper.selectList(
                new LambdaQueryWrapper<SopEdge>()
                        .eq(SopEdge::getWorkflowId, workflowId)
                        .eq(SopEdge::getSourceNodeId, currentNodeId)
                        .orderByAsc(SopEdge::getSortOrder));
        if (edges.isEmpty()) {
            return null;
        }
        // 匹配 conditionExpr == result 的边
        for (SopEdge edge : edges) {
            if (edge.getConditionExpr() != null && edge.getConditionExpr().equals(result)) {
                return nodeMapper.selectById(edge.getTargetNodeId());
            }
        }
        // 无匹配走最后一条边（默认分支）
        return nodeMapper.selectById(edges.get(edges.size() - 1).getTargetNodeId());
    }

    /** 创建执行记录 */
    public void createExecRecord(Long taskId, SopNode node) {
        SopTaskExec exec = new SopTaskExec();
        exec.setTaskId(taskId);
        exec.setNodeId(node.getId());
        exec.setStatus(TaskExecStatus.PENDING.getCode());
        exec.setCreateTime(LocalDateTime.now());
        // 解析assigneeId（可能是逗号分隔的多人）
        if (node.getAssigneeId() != null) {
            exec.setAssigneeId(Long.parseLong(node.getAssigneeId().split(",")[0]));
        }
        taskExecMapper.insert(exec);
    }

    @Transactional
    public void terminate(Long id, Long operatorId) {
        SopTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        TaskStatus current = TaskStatus.of(task.getStatus());
        if (!current.canTransitTo(TaskStatus.TERMINATED)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        String fromStatus = current.getCode();
        task.setStatus(TaskStatus.TERMINATED.getCode());
        task.setCompleteTime(LocalDateTime.now());
        taskMapper.updateById(task);

        writeLog(task.getId(), null, null, operatorId, "TERMINATE",
                fromStatus, TaskStatus.TERMINATED.getCode(), null);
        sopNotifier.send(task, fromStatus, task.getStatus(), operatorId);
    }

    /** 我的待办 */
    public PageResponse<TaskExecVO> myTodo(Long userId, int pageNum, int pageSize) {
        Page<SopTaskExec> page = new Page<>(pageNum, pageSize);
        taskExecMapper.selectPage(page,
                new LambdaQueryWrapper<SopTaskExec>()
                        .eq(SopTaskExec::getAssigneeId, userId)
                        .eq(SopTaskExec::getStatus, TaskExecStatus.PENDING.getCode())
                        .orderByDesc(SopTaskExec::getCreateTime));
        List<TaskExecVO> list = page.getRecords().stream()
                .map(this::toExecVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    /** 我的已办 */
    public PageResponse<TaskExecVO> myDone(Long userId, int pageNum, int pageSize) {
        Page<SopTaskExec> page = new Page<>(pageNum, pageSize);
        taskExecMapper.selectPage(page,
                new LambdaQueryWrapper<SopTaskExec>()
                        .eq(SopTaskExec::getAssigneeId, userId)
                        .eq(SopTaskExec::getStatus, TaskExecStatus.DONE.getCode())
                        .orderByDesc(SopTaskExec::getEndTime));
        List<TaskExecVO> list = page.getRecords().stream()
                .map(this::toExecVO).collect(Collectors.toList());
        return PageResponse.of(list, page.getTotal(), pageNum, pageSize);
    }

    public void writeLog(Long taskId, Long taskExecId, Long nodeId,
                         Long operatorId, String action, String from, String to, String remark) {
        SopOperationLog log = new SopOperationLog();
        log.setTaskId(taskId);
        log.setTaskExecId(taskExecId);
        log.setNodeId(nodeId);
        log.setOperatorId(operatorId);
        if (operatorId != null) {
            SysUser user = userMapper.selectById(operatorId);
            if (user != null) {
                log.setOperatorName(user.getRealName());
            }
        }
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setRemark(remark);
        log.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private SopTaskVO toSimpleVO(SopTask task) {
        SopTaskVO vo = new SopTaskVO();
        vo.setId(task.getId());
        vo.setTaskCode(task.getTaskCode());
        vo.setTaskName(task.getTaskName());
        vo.setTemplateId(task.getTemplateId());
        vo.setWorkflowId(task.getWorkflowId());
        vo.setStatus(task.getStatus());
        vo.setStatusDesc(TaskStatus.of(task.getStatus()).getDesc());
        vo.setCreateBy(task.getCreateBy());
        vo.setCreateTime(task.getCreateTime());
        vo.setCompleteTime(task.getCompleteTime());
        SopWorkflow wf = workflowMapper.selectById(task.getWorkflowId());
        if (wf != null) {
            vo.setWorkflowName(wf.getWorkflowName());
        }
        return vo;
    }

    private SopTaskVO toDetailVO(SopTask task) {
        SopTaskVO vo = toSimpleVO(task);
        List<SopTaskExec> execs = taskExecMapper.selectList(
                new LambdaQueryWrapper<SopTaskExec>()
                        .eq(SopTaskExec::getTaskId, task.getId())
                        .orderByAsc(SopTaskExec::getCreateTime));
        vo.setExecRecords(execs.stream().map(this::toExecVO).collect(Collectors.toList()));
        List<SopOperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<SopOperationLog>()
                        .eq(SopOperationLog::getTaskId, task.getId())
                        .orderByAsc(SopOperationLog::getCreateTime));
        vo.setOperationLogs(logs.stream().map(this::toLogVO).collect(Collectors.toList()));
        return vo;
    }

    private TaskExecVO toExecVO(SopTaskExec exec) {
        TaskExecVO vo = new TaskExecVO();
        vo.setId(exec.getId());
        vo.setTaskId(exec.getTaskId());
        vo.setNodeId(exec.getNodeId());
        vo.setAssigneeId(exec.getAssigneeId());
        vo.setStatus(exec.getStatus());
        vo.setResult(exec.getResult());
        vo.setFeedbackData(exec.getFeedbackData());
        vo.setStartTime(exec.getStartTime());
        vo.setEndTime(exec.getEndTime());
        vo.setCreateTime(exec.getCreateTime());
        SopNode node = nodeMapper.selectById(exec.getNodeId());
        if (node != null) {
            vo.setNodeName(node.getNodeName());
            vo.setNodeType(node.getNodeType());
        }
        if (exec.getAssigneeId() != null) {
            SysUser user = userMapper.selectById(exec.getAssigneeId());
            if (user != null) {
                vo.setAssigneeName(user.getRealName());
            }
        }
        return vo;
    }

    private OperationLogVO toLogVO(SopOperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setTaskId(log.getTaskId());
        vo.setTaskExecId(log.getTaskExecId());
        vo.setNodeId(log.getNodeId());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(log.getOperatorName());
        vo.setAction(log.getAction());
        vo.setFromStatus(log.getFromStatus());
        vo.setToStatus(log.getToStatus());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        if (log.getNodeId() != null) {
            SopNode node = nodeMapper.selectById(log.getNodeId());
            if (node != null) {
                vo.setNodeName(node.getNodeName());
            }
        }
        return vo;
    }
}

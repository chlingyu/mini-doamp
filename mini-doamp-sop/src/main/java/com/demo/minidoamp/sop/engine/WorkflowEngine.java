package com.demo.minidoamp.sop.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.core.entity.SopNode;
import com.demo.minidoamp.core.entity.SopTask;
import com.demo.minidoamp.core.entity.SopTaskExec;
import com.demo.minidoamp.core.enums.ActionType;
import com.demo.minidoamp.core.enums.TaskExecStatus;
import com.demo.minidoamp.core.enums.TaskStatus;
import com.demo.minidoamp.core.mapper.SopNodeMapper;
import com.demo.minidoamp.core.mapper.SopTaskExecMapper;
import com.demo.minidoamp.core.mapper.SopTaskMapper;
import com.demo.minidoamp.sop.service.SopTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WorkflowEngine {

    private final SopTaskExecMapper taskExecMapper;
    private final SopTaskMapper taskMapper;
    private final SopNodeMapper nodeMapper;
    private final SopTaskService taskService;
    private final SopNotifier sopNotifier;
    private final Map<String, NodeHandler> handlerMap;

    public WorkflowEngine(SopTaskExecMapper taskExecMapper,
                           SopTaskMapper taskMapper,
                           SopNodeMapper nodeMapper,
                           @Lazy SopTaskService taskService,
                           SopNotifier sopNotifier,
                           List<NodeHandler> handlers) {
        this.taskExecMapper = taskExecMapper;
        this.taskMapper = taskMapper;
        this.nodeMapper = nodeMapper;
        this.taskService = taskService;
        this.sopNotifier = sopNotifier;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(NodeHandler::getNodeType, Function.identity()));
    }

    @Transactional
    public void advance(Long taskExecId, String action, String result,
                        String feedbackData, String remark, Long operatorId) {
        SopTaskExec exec = taskExecMapper.selectById(taskExecId);
        if (exec == null) {
            throw new BusinessException(ErrorCode.TASK_EXEC_NOT_FOUND);
        }
        SopTask task = taskMapper.selectById(exec.getTaskId());
        SopNode currentNode = nodeMapper.selectById(exec.getNodeId());
        if (currentNode == null) {
            throw new BusinessException(ErrorCode.NODE_NOT_FOUND);
        }

        // 状态机校验
        TaskStatus currentStatus = TaskStatus.of(task.getStatus());
        TaskStatus targetStatus = ActionType.REJECT.getCode().equals(action)
                ? TaskStatus.REJECTED : determineNextTaskStatus(task, currentNode, result);
        if (!currentStatus.canTransitTo(targetStatus)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        String fromStatus = task.getStatus();

        // 驳回处理
        if (ActionType.REJECT.getCode().equals(action)) {
            handleReject(exec, task, currentNode, remark, operatorId);
            sendNotification(task, fromStatus, task.getStatus(), operatorId);
            return;
        }

        // 执行当前节点逻辑（策略模式）
        NodeHandler handler = handlerMap.get(currentNode.getNodeType());
        if (handler != null) {
            handler.handle(exec, currentNode, result, feedbackData);
        } else {
            exec.setStatus(TaskExecStatus.DONE.getCode());
            exec.setEndTime(LocalDateTime.now());
        }
        if (exec.getStartTime() == null) {
            exec.setStartTime(LocalDateTime.now());
        }
        taskExecMapper.updateById(exec);

        // 推进到下一节点（分支节点按条件路由）
        advanceToNext(task, currentNode, result);

        // 一条准确的状态变更日志 + MQ 通知
        taskService.writeLog(task.getId(), exec.getId(), currentNode.getId(),
                operatorId, action, fromStatus, task.getStatus(), remark);
        sendNotification(task, fromStatus, task.getStatus(), operatorId);
    }

    private void handleReject(SopTaskExec exec, SopTask task, SopNode node,
                              String remark, Long operatorId) {
        String fromStatus = task.getStatus();

        exec.setStatus(TaskExecStatus.REJECTED.getCode());
        exec.setEndTime(LocalDateTime.now());
        taskExecMapper.updateById(exec);

        task.setStatus(TaskStatus.REJECTED.getCode());
        task.setCompleteTime(LocalDateTime.now());
        taskMapper.updateById(task);

        taskService.writeLog(task.getId(), exec.getId(), node.getId(),
                operatorId, ActionType.REJECT.getCode(),
                fromStatus, TaskStatus.REJECTED.getCode(), remark);
    }

    /** 推进到下一节点，分支节点按 conditionExpr 路由 */
    private void advanceToNext(SopTask task, SopNode currentNode, String result) {
        SopNode nextNode;
        if ("BRANCH".equals(currentNode.getNodeType())) {
            nextNode = taskService.findNextNodeByCondition(
                    task.getWorkflowId(), currentNode.getId(), result);
        } else {
            nextNode = taskService.findNextNode(task.getWorkflowId(), currentNode.getId());
        }
        if (nextNode == null || "END".equals(nextNode.getNodeType())) {
            task.setStatus(TaskStatus.COMPLETED.getCode());
            task.setCompleteTime(LocalDateTime.now());
            taskMapper.updateById(task);
            return;
        }
        taskService.createExecRecord(task.getId(), nextNode);
        String newStatus = "APPROVE".equals(nextNode.getNodeType())
                ? TaskStatus.APPROVING.getCode() : TaskStatus.EXECUTING.getCode();
        task.setStatus(newStatus);
        taskMapper.updateById(task);
    }

    /** 预判推进后的任务状态（用于 canTransitTo 校验，分支节点按条件路由） */
    private TaskStatus determineNextTaskStatus(SopTask task, SopNode currentNode, String result) {
        SopNode nextNode;
        if ("BRANCH".equals(currentNode.getNodeType())) {
            nextNode = taskService.findNextNodeByCondition(
                    task.getWorkflowId(), currentNode.getId(), result);
        } else {
            nextNode = taskService.findNextNode(task.getWorkflowId(), currentNode.getId());
        }
        if (nextNode == null || "END".equals(nextNode.getNodeType())) {
            return TaskStatus.COMPLETED;
        }
        return "APPROVE".equals(nextNode.getNodeType())
                ? TaskStatus.APPROVING : TaskStatus.EXECUTING;
    }

    @Transactional
    public void rollback(Long taskExecId, Long targetNodeId, String remark, Long operatorId) {
        SopTaskExec currentExec = taskExecMapper.selectById(taskExecId);
        if (currentExec == null) {
            throw new BusinessException(ErrorCode.TASK_EXEC_NOT_FOUND);
        }
        SopNode targetNode = nodeMapper.selectById(targetNodeId);
        if (targetNode == null || "START".equals(targetNode.getNodeType())
                || "END".equals(targetNode.getNodeType())) {
            throw new BusinessException(ErrorCode.ROLLBACK_NOT_ALLOWED);
        }
        SopTask task = taskMapper.selectById(currentExec.getTaskId());

        // 校验目标节点必须有已完成的执行记录
        Long doneCount = taskExecMapper.selectCount(
                new LambdaQueryWrapper<SopTaskExec>()
                        .eq(SopTaskExec::getTaskId, task.getId())
                        .eq(SopTaskExec::getNodeId, targetNodeId)
                        .eq(SopTaskExec::getStatus, TaskExecStatus.DONE.getCode()));
        if (doneCount == 0) {
            throw new BusinessException(ErrorCode.ROLLBACK_NOT_ALLOWED);
        }

        String fromStatus = task.getStatus();

        // 将当前及中间的执行记录标记为 ROLLED_BACK（不物理删除）
        List<SopTaskExec> execs = taskExecMapper.selectList(
                new LambdaQueryWrapper<SopTaskExec>()
                        .eq(SopTaskExec::getTaskId, task.getId())
                        .in(SopTaskExec::getStatus,
                                TaskExecStatus.PENDING.getCode(),
                                TaskExecStatus.PROCESSING.getCode(),
                                TaskExecStatus.DONE.getCode())
                        .ge(SopTaskExec::getCreateTime, getTargetExecTime(task.getId(), targetNodeId))
        );
        for (SopTaskExec e : execs) {
            e.setStatus(TaskExecStatus.ROLLED_BACK.getCode());
            e.setEndTime(LocalDateTime.now());
            taskExecMapper.updateById(e);
        }

        // 在目标节点创建新的执行记录
        taskService.createExecRecord(task.getId(), targetNode);

        // 任务回到执行中
        task.setStatus(TaskStatus.EXECUTING.getCode());
        taskMapper.updateById(task);

        taskService.writeLog(task.getId(), currentExec.getId(), targetNode.getId(),
                operatorId, ActionType.ROLLBACK.getCode(),
                fromStatus, TaskStatus.EXECUTING.getCode(), remark);
        sendNotification(task, fromStatus, task.getStatus(), operatorId);
    }

    private LocalDateTime getTargetExecTime(Long taskId, Long targetNodeId) {
        SopTaskExec targetExec = taskExecMapper.selectOne(
                new LambdaQueryWrapper<SopTaskExec>()
                        .eq(SopTaskExec::getTaskId, taskId)
                        .eq(SopTaskExec::getNodeId, targetNodeId)
                        .eq(SopTaskExec::getStatus, TaskExecStatus.DONE.getCode())
                        .orderByDesc(SopTaskExec::getCreateTime)
                        .last("LIMIT 1"));
        return targetExec != null ? targetExec.getCreateTime() : LocalDateTime.MIN;
    }

    private void sendNotification(SopTask task, String fromStatus, String toStatus, Long operatorId) {
        sopNotifier.send(task, fromStatus, toStatus, operatorId);
    }
}

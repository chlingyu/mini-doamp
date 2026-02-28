package com.demo.minidoamp.sop.engine;

import com.demo.minidoamp.core.entity.SopNode;
import com.demo.minidoamp.core.entity.SopTaskExec;
import com.demo.minidoamp.core.enums.TaskExecStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class CopyNodeHandler implements NodeHandler {

    @Override
    public String getNodeType() {
        return "COPY";
    }

    @Override
    public void handle(SopTaskExec exec, SopNode node, String result, String feedbackData) {
        log.info("抄送节点[{}], 通知人={}", node.getNodeName(), node.getAssigneeId());
        exec.setStatus(TaskExecStatus.DONE.getCode());
        exec.setEndTime(LocalDateTime.now());
    }
}

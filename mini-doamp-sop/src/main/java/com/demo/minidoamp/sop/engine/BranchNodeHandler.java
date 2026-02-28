package com.demo.minidoamp.sop.engine;

import com.demo.minidoamp.core.entity.SopNode;
import com.demo.minidoamp.core.entity.SopTaskExec;
import com.demo.minidoamp.core.enums.TaskExecStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class BranchNodeHandler implements NodeHandler {

    @Override
    public String getNodeType() {
        return "BRANCH";
    }

    @Override
    public void handle(SopTaskExec exec, SopNode node, String result, String feedbackData) {
        exec.setResult(result);
        exec.setStatus(TaskExecStatus.DONE.getCode());
        exec.setEndTime(LocalDateTime.now());
        log.info("分支节点[{}]评估完成, result={}", node.getNodeName(), result);
    }
}

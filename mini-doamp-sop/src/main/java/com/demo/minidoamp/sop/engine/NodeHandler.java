package com.demo.minidoamp.sop.engine;

import com.demo.minidoamp.core.entity.SopNode;
import com.demo.minidoamp.core.entity.SopTaskExec;

/**
 * 节点处理器策略接口，不同节点类型有不同实现
 */
public interface NodeHandler {

    /** 支持的节点类型 */
    String getNodeType();

    /** 执行节点逻辑 */
    void handle(SopTaskExec exec, SopNode node, String result, String feedbackData);
}

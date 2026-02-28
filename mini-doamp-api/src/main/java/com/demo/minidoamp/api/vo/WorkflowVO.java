package com.demo.minidoamp.api.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowVO {

    private Long id;
    private String workflowCode;
    private String workflowName;
    private Integer version;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private List<NodeVO> nodes;
    private List<EdgeVO> edges;

    @Data
    public static class NodeVO {
        private Long id;
        private String nodeCode;
        private String nodeName;
        private String nodeType;
        private String assigneeType;
        private String assigneeId;
        private Integer sortOrder;
        private Integer xPos;
        private Integer yPos;
        private String properties;
    }

    @Data
    public static class EdgeVO {
        private Long id;
        private Long sourceNodeId;
        private Long targetNodeId;
        private String conditionExpr;
        private Integer sortOrder;
    }
}

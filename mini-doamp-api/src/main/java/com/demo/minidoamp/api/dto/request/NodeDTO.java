package com.demo.minidoamp.api.dto.request;

import lombok.Data;

@Data
public class NodeDTO {
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

package com.demo.minidoamp.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已禁用"),
    USERNAME_EXISTS(1003, "用户名已存在"),
    PASSWORD_ERROR(1004, "密码错误"),
    DEPT_NOT_FOUND(1005, "部门不存在"),
    ROLE_NOT_FOUND(1006, "角色不存在"),
    DEPT_HAS_USERS(1007, "部门下存在用户，无法删除"),
    DEPT_HAS_CHILDREN(1008, "部门下存在子部门，无法删除"),
    ROLE_HAS_USERS(1009, "角色下存在用户，无法删除"),

    // 预警相关 2xxx
    INDEX_NOT_FOUND(2001, "指标不存在"),
    RULE_NOT_FOUND(2002, "规则不存在"),
    INDEX_CODE_EXISTS(2003, "指标编码已存在"),
    CUSTOM_SQL_INVALID(2004, "自定义SQL不合法"),
    INVALID_INDEX_TYPE(2005, "非法指标类型"),
    INVALID_COMPARE_TYPE(2006, "非法比较方式"),
    THRESHOLD_LIMIT_REQUIRED(2007, "阈值上下限缺失"),

    // SOP相关 3xxx
    WORKFLOW_NOT_FOUND(3001, "流程定义不存在"),
    TASK_NOT_FOUND(3002, "任务不存在"),
    INVALID_STATUS_TRANSITION(3003, "非法状态转换"),
    NODE_NOT_FOUND(3004, "节点不存在"),

    // 消息相关 4xxx
    MSG_SEND_FAILED(4001, "消息发送失败"),
    MSG_ALREADY_SENT(4002, "消息已发送，不可重复"),

    // 字典相关 5xxx
    DICT_NOT_FOUND(5001, "字典不存在"),
    DICT_CODE_EXISTS(5002, "字典编码已存在");

    private final int code;
    private final String msg;
}

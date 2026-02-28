package com.demo.minidoamp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum TaskStatus {

    CREATED("CREATED", "已创建"),
    PENDING_ASSIGN("PENDING_ASSIGN", "待分配"),
    EXECUTING("EXECUTING", "执行中"),
    APPROVING("APPROVING", "审批中"),
    COMPLETED("COMPLETED", "已完成"),
    REJECTED("REJECTED", "已驳回"),
    TERMINATED("TERMINATED", "已终止");

    private final String code;
    private final String desc;

    /** 状态转换矩阵：key=当前状态，value=允许转换到的目标状态集合 */
    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS;

    static {
        Map<TaskStatus, Set<TaskStatus>> m = new EnumMap<>(TaskStatus.class);
        m.put(CREATED, EnumSet.of(PENDING_ASSIGN, TERMINATED));
        m.put(PENDING_ASSIGN, EnumSet.of(EXECUTING, TERMINATED));
        m.put(EXECUTING, EnumSet.of(EXECUTING, APPROVING, COMPLETED, TERMINATED));
        m.put(APPROVING, EnumSet.of(APPROVING, EXECUTING, COMPLETED, REJECTED, TERMINATED));
        m.put(COMPLETED, Collections.emptySet());
        m.put(REJECTED, Collections.emptySet());
        m.put(TERMINATED, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(m);
    }

    public boolean canTransitTo(TaskStatus target) {
        return TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    public static TaskStatus of(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown TaskStatus: " + code));
    }
}

-- Mini DOAMP 全量建表 SQL
-- 数据库: MySQL 8.0 | 字符集: utf8mb4 | 引擎: InnoDB

-- ========== 1. 预警引擎表 ==========

CREATE TABLE IF NOT EXISTS t_warn_index (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    index_code  VARCHAR(64)  NOT NULL COMMENT '指标编码',
    index_name  VARCHAR(128) NOT NULL COMMENT '指标名称',
    index_type  VARCHAR(32)  NOT NULL COMMENT '指标类型',
    data_table  VARCHAR(128) COMMENT '数据来源表名',
    data_column VARCHAR(128) COMMENT '数据来源字段',
    group_column VARCHAR(128) COMMENT '分组字段',
    custom_sql  TEXT         COMMENT '自定义SQL',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    remark      VARCHAR(256) COMMENT '备注',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_index_code (index_code),
    KEY idx_index_type (index_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警指标主表';

CREATE TABLE IF NOT EXISTS t_warn_threshold (
    id           BIGINT        PRIMARY KEY AUTO_INCREMENT,
    index_id     BIGINT        NOT NULL COMMENT '关联指标ID',
    level        TINYINT       NOT NULL COMMENT '预警级别 1一般 2重要 3紧急',
    upper_limit  DECIMAL(18,4) COMMENT '阈值上限',
    lower_limit  DECIMAL(18,4) COMMENT '阈值下限',
    compare_type VARCHAR(16)   NOT NULL COMMENT '比较方式',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_index_id (index_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警阈值子表';

CREATE TABLE IF NOT EXISTS t_warn_rule (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    rule_name    VARCHAR(128) NOT NULL COMMENT '规则名称',
    index_id     BIGINT       NOT NULL COMMENT '关联指标ID',
    notify_type  VARCHAR(64)  COMMENT '通知方式(SMS,EMAIL,WXWORK)',
    receiver_ids VARCHAR(512) COMMENT '接收人ID列表',
    cron_expr    VARCHAR(64)  COMMENT '调度表达式',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_index_id (index_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';

CREATE TABLE IF NOT EXISTS t_warn_record (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    rule_id         BIGINT        NOT NULL COMMENT '关联规则ID',
    index_id        BIGINT        NOT NULL COMMENT '关联指标ID',
    index_type      VARCHAR(32)   NOT NULL COMMENT '指标类型',
    warn_level      TINYINT       NOT NULL COMMENT '预警级别',
    current_value   DECIMAL(18,4) COMMENT '当前值',
    threshold_value VARCHAR(64)   COMMENT '触发阈值描述',
    group_key       VARCHAR(128)  COMMENT '分组键',
    warn_time       DATETIME      NOT NULL COMMENT '预警时间',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rule_id (rule_id),
    KEY idx_warn_time (warn_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';

-- ========== 2. 消息推送表 ==========

CREATE TABLE IF NOT EXISTS t_msg_record (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
    msg_id           VARCHAR(64)  NOT NULL COMMENT '消息唯一ID(UUID)',
    warn_record_id   BIGINT       COMMENT '关联预警记录ID',
    notify_type      VARCHAR(16)  NOT NULL COMMENT 'SMS/EMAIL/WXWORK',
    receiver_id      BIGINT       COMMENT '接收人ID',
    receiver_name    VARCHAR(64)  COMMENT '接收人姓名',
    receiver_contact VARCHAR(128) COMMENT '联系方式',
    title            VARCHAR(256) COMMENT '消息标题',
    content          TEXT         COMMENT '消息内容',
    status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED/RETRYING/ALARM',
    retry_count      INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    fail_reason      VARCHAR(512) COMMENT '失败原因',
    send_time        DATETIME     COMMENT '发送时间',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_id (msg_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息流水表';

-- ========== 3. 指标数据表 ==========

CREATE TABLE IF NOT EXISTS t_index_running (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    index_code  VARCHAR(64)   NOT NULL COMMENT '指标编码',
    index_value DECIMAL(18,4) NOT NULL COMMENT '指标值',
    data_date   DATE          NOT NULL COMMENT '数据日期',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_code_date (index_code, data_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运行类指标数据';

CREATE TABLE IF NOT EXISTS t_index_operation (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    index_code  VARCHAR(64)   NOT NULL COMMENT '指标编码',
    index_value DECIMAL(18,4) NOT NULL COMMENT '指标值',
    data_date   DATE          NOT NULL COMMENT '数据日期',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_code_date (index_code, data_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营类指标数据';

CREATE TABLE IF NOT EXISTS t_index_group (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    index_code  VARCHAR(64)   NOT NULL COMMENT '指标编码',
    group_type  VARCHAR(32)   NOT NULL COMMENT 'BANK/CHANNEL/EMPLOYEE/BRANCH',
    group_key   VARCHAR(128)  NOT NULL COMMENT '分组键',
    group_name  VARCHAR(128)  COMMENT '分组名称',
    index_value DECIMAL(18,4) NOT NULL COMMENT '指标值',
    data_date   DATE          NOT NULL COMMENT '数据日期',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_code_type_date (index_code, group_type, data_date),
    KEY idx_group_key (group_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分组类指标数据';

-- ========== 4. SOP 工作流表 ==========

CREATE TABLE IF NOT EXISTS t_sop_workflow (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    workflow_code VARCHAR(64)  NOT NULL COMMENT '流程编码',
    workflow_name VARCHAR(128) NOT NULL COMMENT '流程名称',
    version       INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布 2已停用',
    remark        VARCHAR(256) COMMENT '备注',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_by     BIGINT       COMMENT '创建人ID',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_workflow_code_version (workflow_code, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

CREATE TABLE IF NOT EXISTS t_sop_node (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    workflow_id   BIGINT       NOT NULL COMMENT '关联流程ID',
    node_code     VARCHAR(64)  NOT NULL COMMENT '节点编码',
    node_name     VARCHAR(128) NOT NULL COMMENT '节点名称',
    node_type     VARCHAR(16)  NOT NULL COMMENT 'START/PROCESS/APPROVE/COPY/BRANCH/END',
    assignee_type VARCHAR(16)  COMMENT '执行人类型 USER/ROLE/DEPT',
    assignee_id   VARCHAR(256) COMMENT '执行人ID(可多个)',
    sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    x_pos         INT          DEFAULT 0 COMMENT 'X6画布X坐标',
    y_pos         INT          DEFAULT 0 COMMENT 'X6画布Y坐标',
    properties    JSON         COMMENT '节点扩展属性',
    KEY idx_workflow_id (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点表';

CREATE TABLE IF NOT EXISTS t_sop_edge (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    workflow_id    BIGINT       NOT NULL COMMENT '关联流程ID',
    source_node_id BIGINT       NOT NULL COMMENT '源节点ID',
    target_node_id BIGINT       NOT NULL COMMENT '目标节点ID',
    condition_expr VARCHAR(256) COMMENT '分支条件表达式',
    sort_order     INT          NOT NULL DEFAULT 0,
    KEY idx_workflow_id (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程连线表';

CREATE TABLE IF NOT EXISTS t_sop_task_template (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    template_name   VARCHAR(128) NOT NULL COMMENT '模板名称',
    workflow_id     BIGINT       NOT NULL COMMENT '关联流程定义ID',
    content_params  JSON         COMMENT '任务内容参数',
    feedback_params JSON         COMMENT '反馈参数',
    trigger_type    VARCHAR(16)  NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/CRON',
    cron_expr       VARCHAR(64)  COMMENT '定时表达式',
    status          TINYINT      NOT NULL DEFAULT 1,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_workflow_id (workflow_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务模板表';

CREATE TABLE IF NOT EXISTS t_sop_task (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    task_code     VARCHAR(64)  NOT NULL COMMENT '任务编号',
    task_name     VARCHAR(128) NOT NULL COMMENT '任务名称',
    template_id   BIGINT       COMMENT '关联任务模板ID',
    workflow_id   BIGINT       NOT NULL COMMENT '关联流程定义ID',
    status        VARCHAR(16)  NOT NULL DEFAULT 'CREATED' COMMENT '任务状态',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_by     BIGINT       COMMENT '创建人ID',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    complete_time DATETIME     COMMENT '完成时间',
    UNIQUE KEY uk_task_code (task_code),
    KEY idx_status (status),
    KEY idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务实例表';

CREATE TABLE IF NOT EXISTS t_sop_task_exec (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    task_id       BIGINT       NOT NULL COMMENT '关联任务ID',
    node_id       BIGINT       NOT NULL COMMENT '当前节点ID',
    assignee_id   BIGINT       COMMENT '执行人ID',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/DONE/REJECTED/ROLLED_BACK',
    result        TEXT         COMMENT '执行结果/审批意见',
    feedback_data JSON         COMMENT '反馈数据',
    start_time    DATETIME     COMMENT '开始时间',
    end_time      DATETIME     COMMENT '结束时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_id (task_id),
    KEY idx_assignee_id (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行表';

CREATE TABLE IF NOT EXISTS t_sop_operation_log (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    task_id       BIGINT       NOT NULL COMMENT '关联任务ID',
    task_exec_id  BIGINT       COMMENT '关联执行ID',
    node_id       BIGINT       COMMENT '节点ID',
    operator_id   BIGINT       COMMENT '操作人ID',
    operator_name VARCHAR(64)  COMMENT '操作人姓名',
    action        VARCHAR(32)  NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/ROLLBACK/TERMINATE',
    from_status   VARCHAR(16)  COMMENT '变更前状态',
    to_status     VARCHAR(16)  COMMENT '变更后状态',
    remark        VARCHAR(512) COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_id (task_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作流水表';

-- ========== 5. 系统基础表 ==========

CREATE TABLE IF NOT EXISTS t_sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码(BCrypt)',
    real_name   VARCHAR(64)  COMMENT '真实姓名',
    phone       VARCHAR(16)  COMMENT '手机号',
    email       VARCHAR(64)  COMMENT '邮箱',
    dept_id     BIGINT       COMMENT '所属部门ID',
    role_id     BIGINT       COMMENT '角色ID',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    KEY idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS t_sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(64)  NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    status      TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(256) COMMENT '备注',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS t_sys_dept (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    dept_name   VARCHAR(64)  NOT NULL COMMENT '部门名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '上级部门ID(0为顶级)',
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE IF NOT EXISTS t_sys_dict (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    dict_code   VARCHAR(64)  NOT NULL COMMENT '字典编码',
    dict_name   VARCHAR(128) NOT NULL COMMENT '字典名称',
    status      TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(256) COMMENT '备注',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典主表';

CREATE TABLE IF NOT EXISTS t_sys_dict_item (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    dict_id     BIGINT       NOT NULL COMMENT '关联字典ID',
    item_value  VARCHAR(64)  NOT NULL COMMENT '字典项值',
    item_label  VARCHAR(128) NOT NULL COMMENT '字典项标签',
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_dict_id (dict_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- ========== 6. 分布式锁表 ==========

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  PRIMARY KEY COMMENT '锁名称',
    lock_until TIMESTAMP(3) NOT NULL COMMENT '锁持有截止时间',
    locked_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加锁时间',
    locked_by  VARCHAR(255) NOT NULL COMMENT '加锁实例标识'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ShedLock分布式锁表';

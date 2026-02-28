-- Mini DOAMP H2 兼容建表 SQL
-- 数据库: H2 (MODE=MySQL) | 用于多库适配演示

-- ========== 1. 预警引擎表 ==========

CREATE TABLE IF NOT EXISTS t_warn_index (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    index_code  VARCHAR(64)  NOT NULL,
    index_name  VARCHAR(128) NOT NULL,
    index_type  VARCHAR(32)  NOT NULL,
    data_table  VARCHAR(128),
    data_column VARCHAR(128),
    group_column VARCHAR(128),
    custom_sql  TEXT,
    status      TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(256),
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_index_code ON t_warn_index(index_code);
CREATE INDEX IF NOT EXISTS idx_index_type ON t_warn_index(index_type);

CREATE TABLE IF NOT EXISTS t_warn_threshold (
    id           BIGINT        PRIMARY KEY AUTO_INCREMENT,
    index_id     BIGINT        NOT NULL,
    level        TINYINT       NOT NULL,
    upper_limit  DECIMAL(18,4),
    lower_limit  DECIMAL(18,4),
    compare_type VARCHAR(16)   NOT NULL,
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_threshold_index_id ON t_warn_threshold(index_id);

CREATE TABLE IF NOT EXISTS t_warn_rule (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    rule_name    VARCHAR(128) NOT NULL,
    index_id     BIGINT       NOT NULL,
    notify_type  VARCHAR(64),
    receiver_ids VARCHAR(512),
    cron_expr    VARCHAR(64),
    status       TINYINT      NOT NULL DEFAULT 1,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_rule_index_id ON t_warn_rule(index_id);
CREATE INDEX IF NOT EXISTS idx_rule_status ON t_warn_rule(status);

CREATE TABLE IF NOT EXISTS t_warn_record (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    rule_id         BIGINT        NOT NULL,
    index_id        BIGINT        NOT NULL,
    index_type      VARCHAR(32)   NOT NULL,
    warn_level      TINYINT       NOT NULL,
    current_value   DECIMAL(18,4),
    threshold_value VARCHAR(64),
    group_key       VARCHAR(128),
    warn_time       DATETIME      NOT NULL,
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_record_rule_id ON t_warn_record(rule_id);
CREATE INDEX IF NOT EXISTS idx_record_warn_time ON t_warn_record(warn_time);

-- ========== 2. 消息推送表 ==========

CREATE TABLE IF NOT EXISTS t_msg_record (
    id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
    msg_id           VARCHAR(64)  NOT NULL,
    warn_record_id   BIGINT,
    notify_type      VARCHAR(16)  NOT NULL,
    receiver_id      BIGINT,
    receiver_name    VARCHAR(64),
    receiver_contact VARCHAR(128),
    title            VARCHAR(256),
    content          TEXT,
    status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    retry_count      INT          NOT NULL DEFAULT 0,
    fail_reason      VARCHAR(512),
    send_time        DATETIME,
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_msg_id ON t_msg_record(msg_id);
CREATE INDEX IF NOT EXISTS idx_msg_status ON t_msg_record(status);
CREATE INDEX IF NOT EXISTS idx_msg_create_time ON t_msg_record(create_time);

-- ========== 3. 指标数据表 ==========

CREATE TABLE IF NOT EXISTS t_index_running (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    index_code  VARCHAR(64)   NOT NULL,
    index_value DECIMAL(18,4) NOT NULL,
    data_date   DATE          NOT NULL,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_running_code_date ON t_index_running(index_code, data_date);

CREATE TABLE IF NOT EXISTS t_index_operation (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    index_code  VARCHAR(64)   NOT NULL,
    index_value DECIMAL(18,4) NOT NULL,
    data_date   DATE          NOT NULL,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_operation_code_date ON t_index_operation(index_code, data_date);

CREATE TABLE IF NOT EXISTS t_index_group (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    index_code  VARCHAR(64)   NOT NULL,
    group_type  VARCHAR(32)   NOT NULL,
    group_key   VARCHAR(128)  NOT NULL,
    group_name  VARCHAR(128),
    index_value DECIMAL(18,4) NOT NULL,
    data_date   DATE          NOT NULL,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_group_code_type_date ON t_index_group(index_code, group_type, data_date);
CREATE INDEX IF NOT EXISTS idx_group_group_key ON t_index_group(group_key);

-- ========== 4. SOP 工作流表 ==========

CREATE TABLE IF NOT EXISTS t_sop_workflow (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    workflow_code VARCHAR(64)  NOT NULL,
    workflow_name VARCHAR(128) NOT NULL,
    version       INT          NOT NULL DEFAULT 1,
    status        TINYINT      NOT NULL DEFAULT 0,
    remark        VARCHAR(256),
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_by     BIGINT,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_workflow_code_version ON t_sop_workflow(workflow_code, version);

CREATE TABLE IF NOT EXISTS t_sop_node (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    workflow_id   BIGINT       NOT NULL,
    node_code     VARCHAR(64)  NOT NULL,
    node_name     VARCHAR(128) NOT NULL,
    node_type     VARCHAR(16)  NOT NULL,
    assignee_type VARCHAR(16),
    assignee_id   VARCHAR(256),
    sort_order    INT          NOT NULL DEFAULT 0,
    x_pos         INT          DEFAULT 0,
    y_pos         INT          DEFAULT 0,
    properties    TEXT
);
CREATE INDEX IF NOT EXISTS idx_node_workflow_id ON t_sop_node(workflow_id);

CREATE TABLE IF NOT EXISTS t_sop_edge (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    workflow_id    BIGINT       NOT NULL,
    source_node_id BIGINT       NOT NULL,
    target_node_id BIGINT       NOT NULL,
    condition_expr VARCHAR(256),
    sort_order     INT          NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_edge_workflow_id ON t_sop_edge(workflow_id);

CREATE TABLE IF NOT EXISTS t_sop_task_template (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    template_name   VARCHAR(128) NOT NULL,
    workflow_id     BIGINT       NOT NULL,
    content_params  TEXT,
    feedback_params TEXT,
    trigger_type    VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
    cron_expr       VARCHAR(64),
    status          TINYINT      NOT NULL DEFAULT 1,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tpl_workflow_id ON t_sop_task_template(workflow_id);
CREATE INDEX IF NOT EXISTS idx_tpl_status ON t_sop_task_template(status);

CREATE TABLE IF NOT EXISTS t_sop_task (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    task_code     VARCHAR(64)  NOT NULL,
    task_name     VARCHAR(128) NOT NULL,
    template_id   BIGINT,
    workflow_id   BIGINT       NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'CREATED',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    create_by     BIGINT,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complete_time DATETIME
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_task_code ON t_sop_task(task_code);
CREATE INDEX IF NOT EXISTS idx_task_status ON t_sop_task(status);
CREATE INDEX IF NOT EXISTS idx_task_template_id ON t_sop_task(template_id);

CREATE TABLE IF NOT EXISTS t_sop_task_exec (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    task_id       BIGINT       NOT NULL,
    node_id       BIGINT       NOT NULL,
    assignee_id   BIGINT,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    result        TEXT,
    feedback_data TEXT,
    start_time    DATETIME,
    end_time      DATETIME,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_exec_task_id ON t_sop_task_exec(task_id);
CREATE INDEX IF NOT EXISTS idx_exec_assignee_id ON t_sop_task_exec(assignee_id);

CREATE TABLE IF NOT EXISTS t_sop_operation_log (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    task_id       BIGINT       NOT NULL,
    task_exec_id  BIGINT,
    node_id       BIGINT,
    operator_id   BIGINT,
    operator_name VARCHAR(64),
    action        VARCHAR(32)  NOT NULL,
    from_status   VARCHAR(16),
    to_status     VARCHAR(16),
    remark        VARCHAR(512),
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_oplog_task_id ON t_sop_operation_log(task_id);
CREATE INDEX IF NOT EXISTS idx_oplog_create_time ON t_sop_operation_log(create_time);

-- ========== 5. 系统基础表 ==========

CREATE TABLE IF NOT EXISTS t_sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL,
    password    VARCHAR(128) NOT NULL,
    real_name   VARCHAR(64),
    phone       VARCHAR(16),
    email       VARCHAR(64),
    dept_id     BIGINT,
    role_id     BIGINT,
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_username ON t_sys_user(username);
CREATE INDEX IF NOT EXISTS idx_user_dept_id ON t_sys_user(dept_id);

CREATE TABLE IF NOT EXISTS t_sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(64)  NOT NULL,
    role_name   VARCHAR(64)  NOT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(256),
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_code ON t_sys_role(role_code);

CREATE TABLE IF NOT EXISTS t_sys_dept (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    dept_name   VARCHAR(64)  NOT NULL,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_dept_parent_id ON t_sys_dept(parent_id);

CREATE TABLE IF NOT EXISTS t_sys_dict (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    dict_code   VARCHAR(64)  NOT NULL,
    dict_name   VARCHAR(128) NOT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(256),
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dict_code ON t_sys_dict(dict_code);

CREATE TABLE IF NOT EXISTS t_sys_dict_item (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    dict_id     BIGINT       NOT NULL,
    item_value  VARCHAR(64)  NOT NULL,
    item_label  VARCHAR(128) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_dictitem_dict_id ON t_sys_dict_item(dict_id);

-- ========== 6. 分布式锁表 ==========

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by  VARCHAR(255) NOT NULL
);

-- ========== 7. 定时任务执行日志表 ==========

CREATE TABLE IF NOT EXISTS t_job_exec_log (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    job_name    VARCHAR(64)  NOT NULL,
    job_param   VARCHAR(512),
    status      TINYINT      NOT NULL,
    message     VARCHAR(1024),
    cost_ms     BIGINT,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_joblog_job_name ON t_job_exec_log(job_name);
CREATE INDEX IF NOT EXISTS idx_joblog_create_time ON t_job_exec_log(create_time);

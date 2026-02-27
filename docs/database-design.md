# Mini DOAMP 数据库设计文档

> 数据库：MySQL 8.0 | 字符集：utf8mb4 | 引擎：InnoDB

### 通用字段约定

以下业务主表统一包含逻辑删除字段，不在各表中重复列出：

| 字段 | 类型 | 说明 |
|------|------|------|
| deleted | tinyint | 逻辑删除（0正常 1已删除，默认0） |

适用表：t_warn_index、t_warn_rule、t_sop_workflow、t_sop_task_template、t_sop_task、t_sys_user、t_sys_role、t_sys_dept、t_sys_dict

> 记录表（t_warn_record、t_msg_record、t_sop_operation_log）和数据表（t_index_*）不做逻辑删除。

## 1. 预警引擎表

### 1.1 t_warn_index（预警指标主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| index_code | varchar(64) | 指标编码（唯一） |
| index_name | varchar(128) | 指标名称 |
| index_type | varchar(32) | 指标类型（RUNNING/OPERATION/BANK/CHANNEL/EMPLOYEE/BRANCH/CUSTOM_SQL） |
| data_table | varchar(128) | 数据来源表名 |
| data_column | varchar(128) | 数据来源字段 |
| group_column | varchar(128) | 分组字段（银行/渠道/员工等分组查询用） |
| custom_sql | text | 自定义SQL（仅 CUSTOM_SQL 类型） |
| status | tinyint | 状态（0禁用 1启用） |
| remark | varchar(256) | 备注 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`uk_index_code (index_code)`, `idx_index_type (index_type)`

### 1.2 t_warn_threshold（预警阈值子表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| index_id | bigint | 关联指标ID |
| level | tinyint | 预警级别（1一般 2重要 3紧急） |
| upper_limit | decimal(18,4) | 阈值上限 |
| lower_limit | decimal(18,4) | 阈值下限 |
| compare_type | varchar(16) | 比较方式（GT/LT/GTE/LTE/EQ/BETWEEN） |
| create_time | datetime | 创建时间 |

索引：`idx_index_id (index_id)`

### 1.3 t_warn_rule（预警规则表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| rule_name | varchar(128) | 规则名称 |
| index_id | bigint | 关联指标ID |
| notify_type | varchar(64) | 通知方式（逗号分隔：SMS,EMAIL,WXWORK） |
| receiver_ids | varchar(512) | 接收人ID列表（逗号分隔） |
| cron_expr | varchar(64) | 调度表达式 |
| status | tinyint | 状态（0禁用 1启用） |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`idx_index_id (index_id)`, `idx_status (status)`

### 1.4 t_warn_record（预警记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| rule_id | bigint | 关联规则ID |
| index_id | bigint | 关联指标ID |
| index_type | varchar(32) | 指标类型（冗余，方便查询） |
| warn_level | tinyint | 预警级别 |
| current_value | decimal(18,4) | 当前值 |
| threshold_value | varchar(64) | 触发阈值描述 |
| group_key | varchar(128) | 分组键（银行名/渠道名等） |
| warn_time | datetime | 预警时间 |
| create_time | datetime | 创建时间 |

索引：`idx_rule_id (rule_id)`, `idx_warn_time (warn_time)`

## 2. 消息推送表

### 2.1 t_msg_record（消息流水表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| msg_id | varchar(64) | 消息唯一ID（UUID，用于幂等） |
| warn_record_id | bigint | 关联预警记录ID |
| notify_type | varchar(16) | 通知方式（SMS/EMAIL/WXWORK） |
| receiver_id | bigint | 接收人ID |
| receiver_name | varchar(64) | 接收人姓名 |
| receiver_contact | varchar(128) | 接收人联系方式（手机/邮箱/企微ID） |
| title | varchar(256) | 消息标题 |
| content | text | 消息内容 |
| status | varchar(16) | 状态（PENDING/SENT/FAILED/RETRYING/ALARM） |
| retry_count | int | 已重试次数（最多3次） |
| fail_reason | varchar(512) | 失败原因 |
| send_time | datetime | 发送时间 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`uk_msg_id (msg_id)`, `idx_status (status)`, `idx_create_time (create_time)`

## 3. SOP 工作流表

### 3.1 t_sop_workflow（流程定义表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| workflow_code | varchar(64) | 流程编码（唯一） |
| workflow_name | varchar(128) | 流程名称 |
| version | int | 版本号 |
| status | tinyint | 状态（0草稿 1已发布 2已停用） |
| remark | varchar(256) | 备注 |
| create_by | bigint | 创建人ID |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`uk_workflow_code_version (workflow_code, version)`

### 3.2 t_sop_node（流程节点表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| workflow_id | bigint | 关联流程ID |
| node_code | varchar(64) | 节点编码 |
| node_name | varchar(128) | 节点名称 |
| node_type | varchar(16) | 节点类型（START/PROCESS/APPROVE/COPY/BRANCH/END） |
| assignee_type | varchar(16) | 执行人类型（USER/ROLE/DEPT） |
| assignee_id | varchar(256) | 执行人ID（可多个，逗号分隔） |
| sort_order | int | 排序号 |
| x_pos | int | X6画布X坐标 |
| y_pos | int | X6画布Y坐标 |
| properties | json | 节点扩展属性（JSON） |

索引：`idx_workflow_id (workflow_id)`

### 3.3 t_sop_edge（流程连线表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| workflow_id | bigint | 关联流程ID |
| source_node_id | bigint | 源节点ID |
| target_node_id | bigint | 目标节点ID |
| condition_expr | varchar(256) | 分支条件表达式（仅分支节点出边） |
| sort_order | int | 排序号 |

索引：`idx_workflow_id (workflow_id)`

### 3.4 t_sop_task_template（任务模板表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| template_name | varchar(128) | 模板名称 |
| workflow_id | bigint | 关联流程定义ID |
| content_params | json | 任务内容参数（标题、描述等） |
| feedback_params | json | 反馈参数（执行完后需填写的字段） |
| trigger_type | varchar(16) | 触发方式（MANUAL/CRON） |
| cron_expr | varchar(64) | 定时表达式（CRON类型时） |
| status | tinyint | 状态（0禁用 1启用） |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`idx_workflow_id (workflow_id)`, `idx_status (status)`

### 3.5 t_sop_task（任务实例表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| task_code | varchar(64) | 任务编号（唯一） |
| task_name | varchar(128) | 任务名称 |
| template_id | bigint | 关联任务模板ID |
| workflow_id | bigint | 关联流程定义ID |
| status | varchar(16) | 任务状态（CREATED/PENDING_ASSIGN/EXECUTING/APPROVING/COMPLETED/REJECTED/TERMINATED） |
| create_by | bigint | 创建人ID |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |
| complete_time | datetime | 完成时间 |

索引：`uk_task_code (task_code)`, `idx_status (status)`, `idx_template_id (template_id)`

### 3.6 t_sop_task_exec（任务执行表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| task_id | bigint | 关联任务ID |
| node_id | bigint | 当前节点ID |
| assignee_id | bigint | 执行人ID |
| status | varchar(16) | 执行状态（PENDING/PROCESSING/DONE/REJECTED/ROLLED_BACK） |
| result | text | 执行结果/审批意见 |
| feedback_data | json | 反馈数据（JSON） |
| start_time | datetime | 开始时间 |
| end_time | datetime | 结束时间 |
| create_time | datetime | 创建时间 |

索引：`idx_task_id (task_id)`, `idx_assignee_id (assignee_id)`

### 3.7 t_sop_operation_log（操作流水表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| task_id | bigint | 关联任务ID |
| task_exec_id | bigint | 关联执行ID |
| node_id | bigint | 节点ID |
| operator_id | bigint | 操作人ID |
| operator_name | varchar(64) | 操作人姓名 |
| action | varchar(32) | 操作类型（SUBMIT/APPROVE/REJECT/ROLLBACK/TERMINATE） |
| from_status | varchar(16) | 变更前状态 |
| to_status | varchar(16) | 变更后状态 |
| remark | varchar(512) | 备注 |
| create_time | datetime | 操作时间 |

索引：`idx_task_id (task_id)`, `idx_create_time (create_time)`

## 4. 指标数据表

### 4.1 t_index_running（运行类指标数据）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| index_code | varchar(64) | 指标编码 |
| index_value | decimal(18,4) | 指标值 |
| data_date | date | 数据日期 |
| create_time | datetime | 创建时间 |

索引：`idx_code_date (index_code, data_date)`

### 4.2 t_index_operation（运营类指标数据）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| index_code | varchar(64) | 指标编码 |
| index_value | decimal(18,4) | 指标值 |
| data_date | date | 数据日期 |
| create_time | datetime | 创建时间 |

索引：`idx_code_date (index_code, data_date)`

### 4.3 t_index_group（分组类指标数据）

> 银行类、渠道效能类、员工类、营业部类指标共用此表，通过 group_type 区分

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| index_code | varchar(64) | 指标编码 |
| group_type | varchar(32) | 分组类型（BANK/CHANNEL/EMPLOYEE/BRANCH） |
| group_key | varchar(128) | 分组键（机构名/渠道名/员工工号/营业部名） |
| group_name | varchar(128) | 分组名称（用于展示） |
| index_value | decimal(18,4) | 指标值 |
| data_date | date | 数据日期 |
| create_time | datetime | 创建时间 |

索引：`idx_code_type_date (index_code, group_type, data_date)`, `idx_group_key (group_key)`

## 5. 系统基础表

### 5.1 t_sys_user（用户表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| username | varchar(64) | 用户名（唯一） |
| password | varchar(128) | 密码（BCrypt加密） |
| real_name | varchar(64) | 真实姓名 |
| phone | varchar(16) | 手机号 |
| email | varchar(64) | 邮箱 |
| dept_id | bigint | 所属部门ID |
| role_id | bigint | 角色ID |
| status | tinyint | 状态（0禁用 1启用） |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`uk_username (username)`, `idx_dept_id (dept_id)`

### 5.2 t_sys_role（角色表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| role_code | varchar(64) | 角色编码（唯一） |
| role_name | varchar(64) | 角色名称 |
| status | tinyint | 状态（0禁用 1启用） |
| remark | varchar(256) | 备注 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`uk_role_code (role_code)`

### 5.3 t_sys_dept（部门/组织表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| dept_name | varchar(64) | 部门名称 |
| parent_id | bigint | 上级部门ID（0为顶级） |
| sort_order | int | 排序号 |
| status | tinyint | 状态（0禁用 1启用） |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`idx_parent_id (parent_id)`

### 5.4 t_sys_dict（字典主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| dict_code | varchar(64) | 字典编码（唯一） |
| dict_name | varchar(128) | 字典名称 |
| status | tinyint | 状态（0禁用 1启用） |
| remark | varchar(256) | 备注 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

索引：`uk_dict_code (dict_code)`

### 5.5 t_sys_dict_item（字典项表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 主键 |
| dict_id | bigint | 关联字典ID |
| item_value | varchar(64) | 字典项值 |
| item_label | varchar(128) | 字典项标签 |
| sort_order | int | 排序号 |
| status | tinyint | 状态（0禁用 1启用） |
| create_time | datetime | 创建时间 |

索引：`idx_dict_id (dict_id)`

## 6. 分布式锁表

### 6.1 shedlock（ShedLock 分布式锁表）

| 字段 | 类型 | 说明 |
|------|------|------|
| name | varchar(64) PK | 锁名称（动态生成：`任务类型_${模板ID}`） |
| lock_until | timestamp | 锁持有截止时间 |
| locked_at | timestamp | 加锁时间 |
| locked_by | varchar(255) | 加锁实例标识 |

> 此表由 ShedLock 框架自动维护，锁名称动态生成是本项目的核心亮点（解决同一调度周期不同模板互相抢锁的线上问题）。

## 7. 表关系总览

```
t_warn_index ──1:N──→ t_warn_threshold
t_warn_index ──1:N──→ t_warn_rule
t_warn_rule  ──1:N──→ t_warn_record
t_warn_record──1:N──→ t_msg_record

t_sop_workflow──1:N──→ t_sop_node
t_sop_workflow──1:N──→ t_sop_edge
t_sop_workflow──1:N──→ t_sop_task_template
t_sop_task_template──1:N──→ t_sop_task
t_sop_task   ──1:N──→ t_sop_task_exec
t_sop_task   ──1:N──→ t_sop_operation_log

t_sys_dept   ──1:N──→ t_sys_user
t_sys_role   ──1:N──→ t_sys_user
t_sys_dict   ──1:N──→ t_sys_dict_item
```

共计 **20 张业务表 + 1 张框架表**（shedlock 由 ShedLock 框架自动维护，不需要业务 API），覆盖预警引擎、消息推送、SOP工作流、指标数据、系统基础、分布式锁六大模块。

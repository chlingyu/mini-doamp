# Mini DOAMP API 接口设计文档

> 基础路径：`/api` | 认证方式：JWT Bearer Token | 响应格式：JSON

## 1. 统一规范

### 1.1 响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

### 1.2 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未认证（Token无效或过期） |
| 403 | 无权限 |
| 500 | 服务器内部错误 |
| 1001 | 用户名或密码错误 |
| 1002 | 账号已禁用 |
| 2001 | 预警指标不存在 |
| 2002 | 预警规则不存在 |
| 2003 | 自定义SQL校验失败 |
| 3001 | 流程定义不存在 |
| 3002 | 任务不存在 |
| 3003 | 状态转换非法 |
| 3004 | 回退节点不合法 |
| 4001 | 字典编码已存在 |

### 1.3 分页请求参数

| 参数 | 类型 | 说明 |
|------|------|------|
| pageNum | int | 页码（默认1） |
| pageSize | int | 每页条数（默认10） |

### 1.4 分页响应格式

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

## 2. 认证模块（gateway）

### 2.1 登录

```
POST /api/auth/login
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

响应 data：
| 字段 | 说明 |
|------|------|
| token | JWT Token |
| expireTime | 过期时间 |

### 2.2 获取当前用户信息

```
GET /api/auth/userInfo
```

响应 data：
| 字段 | 说明 |
|------|------|
| id | 用户ID |
| username | 用户名 |
| realName | 真实姓名 |
| roleName | 角色名称 |
| deptName | 部门名称 |

### 2.3 退出登录

```
POST /api/auth/logout
```

## 3. 预警模块（event）

### 3.1 预警指标管理

#### 指标列表（分页）

```
GET /api/warn/index/list?pageNum=1&pageSize=10&indexType=RUNNING&status=1
```

响应 data：分页格式，records 为指标列表

#### 指标详情

```
GET /api/warn/index/{id}
```

#### 新增指标

```
POST /api/warn/index
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| indexCode | string | 是 | 指标编码 |
| indexName | string | 是 | 指标名称 |
| indexType | string | 是 | 指标类型（RUNNING/OPERATION/BANK/CHANNEL/EMPLOYEE/BRANCH/CUSTOM_SQL） |
| dataTable | string | 否 | 数据来源表名 |
| dataColumn | string | 否 | 数据来源字段 |
| groupColumn | string | 否 | 分组字段 |
| customSql | string | 否 | 自定义SQL（仅CUSTOM_SQL类型） |
| thresholds | array | 是 | 阈值列表 |

thresholds 数组元素：
| 字段 | 类型 | 说明 |
|------|------|------|
| level | int | 预警级别（1/2/3） |
| upperLimit | decimal | 阈值上限 |
| lowerLimit | decimal | 阈值下限 |
| compareType | string | 比较方式（GT/LT/GTE/LTE/EQ/BETWEEN） |

#### 修改指标

```
PUT /api/warn/index/{id}
```

请求体同新增

#### 删除指标

```
DELETE /api/warn/index/{id}
```

#### 启用/禁用指标

```
PUT /api/warn/index/{id}/status?status=1
```

### 3.2 预警规则管理

#### 规则列表（分页）

```
GET /api/warn/rule/list?pageNum=1&pageSize=10&status=1
```

#### 新增规则

```
POST /api/warn/rule
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ruleName | string | 是 | 规则名称 |
| indexId | long | 是 | 关联指标ID |
| notifyType | string | 是 | 通知方式（逗号分隔：SMS,EMAIL,WXWORK） |
| receiverIds | string | 是 | 接收人ID列表（逗号分隔） |
| cronExpr | string | 是 | 调度表达式 |

#### 修改规则

```
PUT /api/warn/rule/{id}
```

#### 删除规则

```
DELETE /api/warn/rule/{id}
```

#### 启用/禁用规则

```
PUT /api/warn/rule/{id}/status?status=1
```

#### 手动触发预警检查

```
POST /api/warn/rule/{id}/trigger
```

> 立即执行一次预警检查，不等定时调度

### 3.3 预警记录查询

#### 预警记录列表（分页）

```
GET /api/warn/record/list?pageNum=1&pageSize=10&indexType=RUNNING&warnLevel=3&startTime=&endTime=
```

查询参数：
| 参数 | 类型 | 说明 |
|------|------|------|
| indexType | string | 指标类型（可选） |
| warnLevel | int | 预警级别（可选） |
| startTime | string | 开始时间（可选） |
| endTime | string | 结束时间（可选） |

## 4. 消息模块（event）

### 4.1 消息记录查询（分页）

```
GET /api/msg/record/list?pageNum=1&pageSize=10&status=FAILED&notifyType=SMS
```

查询参数：
| 参数 | 类型 | 说明 |
|------|------|------|
| status | string | 消息状态（PENDING/SENT/FAILED/RETRYING/ALARM，可选） |
| notifyType | string | 通知方式（SMS/EMAIL/WXWORK，可选） |
| startTime | string | 开始时间（可选） |
| endTime | string | 结束时间（可选） |

### 4.2 手动重试发送

```
POST /api/msg/record/{id}/retry
```

> 对 FAILED 或 ALARM 状态的消息手动触发重试

### 4.3 消息统计

```
GET /api/msg/record/stats
```

响应 data：
| 字段 | 说明 |
|------|------|
| totalCount | 总消息数 |
| sentCount | 已发送数 |
| failedCount | 失败数 |
| alarmCount | 告警数（需人工处理） |

## 5. SOP 工作流模块（sop）

### 5.1 流程定义管理

#### 流程列表（分页）

```
GET /api/sop/workflow/list?pageNum=1&pageSize=10&status=1
```

#### 流程详情（含节点和连线）

```
GET /api/sop/workflow/{id}
```

响应 data：
| 字段 | 说明 |
|------|------|
| id | 流程ID |
| workflowCode | 流程编码 |
| workflowName | 流程名称 |
| version | 版本号 |
| status | 状态 |
| nodes | 节点列表（含坐标、属性） |
| edges | 连线列表（含条件表达式） |

#### 保存流程设计（从X6画布提交）

```
POST /api/sop/workflow
```

> 节点和连线均通过 nodeCode 关联。前端设计器为每个节点生成唯一 nodeCode，edges 通过 sourceNodeCode/targetNodeCode 引用节点。后端保存时先批量插入节点（生成 node_id），再根据 nodeCode 映射建立 edge 的 source_node_id/target_node_id 关联。

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| workflowCode | string | 是 | 流程编码 |
| workflowName | string | 是 | 流程名称 |
| nodes | array | 是 | 节点列表 |
| edges | array | 是 | 连线列表 |

nodes 数组元素：
| 字段 | 类型 | 说明 |
|------|------|------|
| nodeCode | string | 节点编码 |
| nodeName | string | 节点名称 |
| nodeType | string | 节点类型（START/PROCESS/APPROVE/COPY/BRANCH/END） |
| assigneeType | string | 执行人类型（USER/ROLE/DEPT） |
| assigneeId | string | 执行人ID |
| sortOrder | int | 排序号 |
| xPos | int | X6画布X坐标 |
| yPos | int | X6画布Y坐标 |
| properties | object | 扩展属性 |

edges 数组元素：
| 字段 | 类型 | 说明 |
|------|------|------|
| sourceNodeCode | string | 源节点编码（对应 nodes 中的 nodeCode） |
| targetNodeCode | string | 目标节点编码（对应 nodes 中的 nodeCode） |
| conditionExpr | string | 分支条件表达式 |
| sortOrder | int | 排序号 |

#### 修改流程设计

```
PUT /api/sop/workflow/{id}
```

请求体同新增

#### 发布流程

```
PUT /api/sop/workflow/{id}/publish
```

#### 停用流程

```
PUT /api/sop/workflow/{id}/disable
```

### 5.2 任务模板管理

#### 任务模板列表（分页）

```
GET /api/sop/task-template/list?pageNum=1&pageSize=10&status=1
```

#### 新增任务模板

```
POST /api/sop/task-template
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateName | string | 是 | 模板名称 |
| workflowId | long | 是 | 关联流程定义ID |
| contentParams | object | 是 | 任务内容参数（标题、描述） |
| feedbackParams | object | 否 | 反馈参数定义 |
| triggerType | string | 是 | 触发方式（MANUAL/CRON） |
| cronExpr | string | 否 | 定时表达式（CRON类型时必填） |

#### 修改任务模板

```
PUT /api/sop/task-template/{id}
```

#### 删除任务模板

```
DELETE /api/sop/task-template/{id}
```

#### 启用/禁用任务模板

```
PUT /api/sop/task-template/{id}/status?status=1
```

### 5.3 任务管理

#### 任务列表（分页）

```
GET /api/sop/task/list?pageNum=1&pageSize=10&status=EXECUTING
```

查询参数：
| 参数 | 类型 | 说明 |
|------|------|------|
| status | string | 任务状态（可选） |
| templateId | long | 任务模板ID（可选） |
| createBy | long | 创建人ID（可选） |

#### 任务详情（含流程图数据和执行记录）

```
GET /api/sop/task/{id}
```

响应 data：
| 字段 | 说明 |
|------|------|
| task | 任务基本信息 |
| workflow | 流程定义（节点+连线，用于X6渲染） |
| currentNodeId | 当前所在节点ID（用于高亮） |
| execRecords | 执行记录列表 |
| operationLogs | 操作流水列表 |

#### 手动创建任务

```
POST /api/sop/task
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateId | long | 是 | 任务模板ID |
| taskName | string | 是 | 任务名称 |

#### 终止任务

```
PUT /api/sop/task/{id}/terminate
```

### 5.4 流程推进（核心引擎接口）

#### 提交/推进节点

```
POST /api/sop/task-exec/{taskExecId}/advance
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| action | string | 是 | 操作类型（SUBMIT/APPROVE/REJECT） |
| result | string | 否 | 执行结果/审批意见 |
| feedbackData | object | 否 | 反馈数据（JSON） |

> 此接口对应 `WorkflowEngine.advance(taskExecId, action, params)`，是整个 SOP 引擎的核心入口。

#### 回退到指定节点

```
POST /api/sop/task-exec/{taskExecId}/rollback
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| targetNodeId | long | 是 | 回退目标节点ID（必须是已完成的节点） |
| remark | string | 否 | 回退原因 |

> 回退时将中间节点的执行记录标记为 ROLLED_BACK（不物理删除），按节点执行栈回溯。

### 5.5 我的待办/已办

#### 我的待办列表

```
GET /api/sop/task-exec/todo?pageNum=1&pageSize=10
```

> 查询当前登录用户 assignee_id 且 status=PENDING 的执行记录

#### 我的已办列表

```
GET /api/sop/task-exec/done?pageNum=1&pageSize=10
```

> 查询当前登录用户 assignee_id 且 status=DONE/REJECTED 的执行记录

## 6. 系统管理模块（doamp）

### 6.1 字典管理

#### 字典列表（分页）

```
GET /api/system/dict/list?pageNum=1&pageSize=10
```

#### 新增字典

```
POST /api/system/dict
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dictCode | string | 是 | 字典编码 |
| dictName | string | 是 | 字典名称 |
| items | array | 否 | 字典项列表 |

#### 修改字典

```
PUT /api/system/dict/{id}
```

#### 删除字典

```
DELETE /api/system/dict/{id}
```

#### 根据字典编码获取字典项（走缓存）

```
GET /api/system/dict/items/{dictCode}
```

> 优先从 Redis 缓存读取，miss 则查 DB 并回填缓存

### 6.2 缓存管理

#### 手动刷新字典缓存

```
POST /api/system/cache/refresh/dict
```

#### 手动刷新指标缓存

```
POST /api/system/cache/refresh/index
```

#### 刷新全部缓存

```
POST /api/system/cache/refresh/all
```

### 6.3 定时任务监控

#### 任务列表

```
GET /api/system/job/list
```

响应 data：
| 字段 | 说明 |
|------|------|
| jobName | 任务名称 |
| cronExpr | 调度表达式 |
| status | 状态（运行中/已停止） |
| lastExecTime | 上次执行时间 |
| nextExecTime | 下次执行时间 |

#### 任务执行日志（分页）

```
GET /api/system/job/log?pageNum=1&pageSize=10&jobName=warn_check
```

## 7. 用户管理模块（gateway）

### 7.1 用户列表（分页）

```
GET /api/system/user/list?pageNum=1&pageSize=10&deptId=1
```

### 7.2 新增用户

```
POST /api/system/user
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |
| realName | string | 是 | 真实姓名 |
| phone | string | 否 | 手机号 |
| email | string | 否 | 邮箱 |
| deptId | long | 否 | 部门ID |
| roleId | long | 否 | 角色ID |

### 7.3 修改用户

```
PUT /api/system/user/{id}
```

### 7.4 删除用户

```
DELETE /api/system/user/{id}
```

### 7.5 重置密码

```
PUT /api/system/user/{id}/resetPwd
```

### 7.6 角色列表

```
GET /api/system/role/list?pageNum=1&pageSize=10
```

### 7.7 新增角色

```
POST /api/system/role
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleCode | string | 是 | 角色编码 |
| roleName | string | 是 | 角色名称 |
| remark | string | 否 | 备注 |

### 7.8 修改角色

```
PUT /api/system/role/{id}
```

### 7.9 删除角色

```
DELETE /api/system/role/{id}
```

### 7.10 部门树

```
GET /api/system/dept/tree
```

> 返回树形结构的部门列表

### 7.11 新增部门

```
POST /api/system/dept
```

请求体：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deptName | string | 是 | 部门名称 |
| parentId | long | 是 | 上级部门ID（0为顶级） |
| sortOrder | int | 否 | 排序号 |

### 7.12 修改部门

```
PUT /api/system/dept/{id}
```

### 7.13 删除部门

```
DELETE /api/system/dept/{id}
```

## 8. 接口汇总

| 模块 | 接口数 | 说明 |
|------|--------|------|
| 认证 | 3 | 登录、用户信息、退出 |
| 预警指标 | 6 | CRUD + 启禁用 |
| 预警规则 | 6 | CRUD + 启禁用 + 手动触发 |
| 预警记录 | 1 | 分页查询 |
| 消息记录 | 3 | 列表 + 重试 + 统计 |
| 流程定义 | 5 | CRUD + 发布 + 停用 |
| 任务模板 | 5 | CRUD + 启禁用 |
| 任务管理 | 4 | 列表 + 详情 + 创建 + 终止 |
| 流程推进 | 2 | 提交/审批 + 回退 |
| 待办/已办 | 2 | 我的待办 + 我的已办 |
| 字典管理 | 5 | CRUD + 按编码查询 |
| 缓存管理 | 3 | 刷新字典/指标/全部 |
| 定时任务 | 2 | 任务列表 + 执行日志 |
| 用户管理 | 5 | CRUD + 重置密码 |
| 角色管理 | 4 | CRUD |
| 部门管理 | 4 | 树查询 + CUD |

共计约 **60 个接口**，覆盖全部业务模块。

> **不提供 API 的表说明：**
> - `t_index_running`、`t_index_operation`、`t_index_group`：指标数据表，由外部系统导入或初始化脚本填充，不提供 CRUD 接口
> - `shedlock`：ShedLock 框架自动维护，无需业务接口

-- Mini DOAMP 初始化数据

-- ========== 1. 管理员账号 ==========
-- 密码: admin123 (BCrypt加密)
INSERT INTO t_sys_role (id, role_code, role_name, remark) VALUES
(1, 'ADMIN', '系统管理员', '拥有全部权限'),
(2, 'OPERATOR', '运营人员', '预警和SOP操作权限');

INSERT INTO t_sys_dept (id, dept_name, parent_id, sort_order) VALUES
(1, '总公司', 0, 1),
(2, '运营部', 1, 1),
(3, '技术部', 1, 2),
(4, '风控部', 1, 3);

-- 密码: admin123 → BCrypt
INSERT INTO t_sys_user (id, username, password, real_name, phone, email, dept_id, role_id) VALUES
(1, 'admin', '$2a$10$P8AVDRDdnIh3OshE4IBkROBx8OaBIh3iLLxgMw.9P76FaaZuQzzCK', '管理员', '13800000001', 'admin@demo.com', 1, 1),
(2, 'operator', '$2a$10$P8AVDRDdnIh3OshE4IBkROBx8OaBIh3iLLxgMw.9P76FaaZuQzzCK', '运营员', '13800000002', 'op@demo.com', 2, 2);

-- ========== 2. 基础字典 ==========
INSERT INTO t_sys_dict (id, dict_code, dict_name) VALUES
(1, 'index_type', '指标类型'),
(2, 'warn_level', '预警级别'),
(3, 'notify_type', '通知方式'),
(4, 'task_status', '任务状态');

INSERT INTO t_sys_dict_item (dict_id, item_value, item_label, sort_order) VALUES
(1, 'RUNNING', '运行类', 1),
(1, 'OPERATION', '运营类', 2),
(1, 'BANK', '银行类', 3),
(1, 'CHANNEL', '渠道效能类', 4),
(1, 'EMPLOYEE', '员工类', 5),
(1, 'BRANCH', '营业部类', 6),
(1, 'CUSTOM_SQL', '自定义SQL类', 7),
(2, '1', '一般', 1),
(2, '2', '重要', 2),
(2, '3', '紧急', 3),
(3, 'SMS', '短信', 1),
(3, 'EMAIL', '邮件', 2),
(3, 'WXWORK', '企业微信', 3);

-- ========== 3. 示例预警指标 ==========
INSERT INTO t_warn_index (id, index_code, index_name, index_type, data_table, data_column) VALUES
(1, 'IDX_SYS_CPU', '系统CPU使用率', 'RUNNING', 't_index_running', 'index_value'),
(2, 'IDX_TRADE_AMT', '交易金额', 'OPERATION', 't_index_operation', 'index_value'),
(3, 'IDX_BANK_BALANCE', '银行余额', 'BANK', 't_index_group', 'index_value');

UPDATE t_warn_index SET group_column = 'group_key' WHERE id = 3;

INSERT INTO t_warn_threshold (index_id, level, upper_limit, lower_limit, compare_type) VALUES
(1, 1, 80.0000, NULL, 'GT'),
(1, 3, 95.0000, NULL, 'GT'),
(2, 2, 1000000.0000, NULL, 'GT');

-- ========== 4. 示例指标数据 ==========
INSERT INTO t_index_running (index_code, index_value, data_date) VALUES
('IDX_SYS_CPU', 75.5000, CURDATE()),
('IDX_SYS_CPU', 92.3000, CURDATE());

INSERT INTO t_index_operation (index_code, index_value, data_date) VALUES
('IDX_TRADE_AMT', 500000.0000, CURDATE()),
('IDX_TRADE_AMT', 1200000.0000, CURDATE());

INSERT INTO t_index_group (index_code, group_type, group_key, group_name, index_value, data_date) VALUES
('IDX_BANK_BALANCE', 'BANK', 'ICBC', '工商银行', 8500000.0000, CURDATE()),
('IDX_BANK_BALANCE', 'BANK', 'CCB', '建设银行', 6200000.0000, CURDATE());

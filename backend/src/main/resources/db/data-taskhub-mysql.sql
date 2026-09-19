-- =============================================
-- 脚本名称: data-taskhub-mysql.sql
-- 功能描述: TaskHub 任务管理平台 MySQL 示例数据初始化脚本（DML）
--           依据 config/DataInitializer.java 的演示数据逻辑转换为可执行 INSERT 语句
-- 数据来源: com.example.taskhub.config.DataInitializer（6 个用户 + 13 条待办）
-- 依赖脚本: init-taskhub-mysql.sql（必须先建表，再执行本脚本写入数据）
-- 适用范围: MySQL 8.0+（兼容 5.7）；仅用于开发 / 测试 / 演示环境
-- 执行方式:
--   方式一: mysql -u root -p taskhub < data-taskhub-mysql.sql
--   方式二: 登录 MySQL 后 USE taskhub; source /path/to/data-taskhub-mysql.sql
-- 执行顺序: 2（在 init-taskhub-mysql.sql 之后；先插用户，再插待办，满足外键依赖）
-- 注意事项:
--   1. 本脚本会先清空 sys_user / todo_item 两张表再写入，请勿在生产环境执行
--   2. 日期采用 CURDATE() 相对表达式，忠实还原 DataInitializer 中 LocalDate.now() 的
--      相对偏移，因此"逾期""今日到期"等场景在任意执行日都成立，便于测试统计功能
--   3. 枚举列（role/status/priority）以字符串枚举名写入（JPA EnumType.STRING），如 'ADMIN'
--   4. 主键显式指定（用户 1-6、待办 1-13），assignee_id 直接引用用户 ID，便于核对
--   5. 全部 DML 包裹在事务中，失败可整体回滚
--
-- 数据覆盖度:
--   sys_user  : 6 条 | 角色 ADMIN/MANAGER/MEMBER/GUEST | 状态 ACTIVE/DISABLED
--   todo_item : 13 条 | 状态 PENDING/IN_PROGRESS/DONE/CANCELLED
--                      | 优先级 LOW/MEDIUM/HIGH/URGENT
--                      | 含 2 条逾期(id=1,2)、1 条未分配(id=10)、2 条已完成带 completed_at(id=11,12)
--
-- 作者: TaskHub
-- 创建时间: 2026-09-19
-- =============================================

-- 设置客户端字符集，确保中文数据正确写入
SET NAMES utf8mb4;

-- 切换到目标数据库（与 init-taskhub-mysql.sql 保持一致）
USE `taskhub`;

-- 开启事务：清理 + 写入作为一个原子操作，中途失败整体回滚
START TRANSACTION;

-- =============================================
-- 步骤一: 清理旧演示数据
-- 说明: 按外键依赖逆序删除——先删子表 todo_item，再删父表 sys_user，避免外键约束报错
-- 警告: 该操作会移除两张表的全部现有数据，仅适用于开发/测试环境重置
-- =============================================
DELETE FROM `todo_item`;
DELETE FROM `sys_user`;

-- =============================================
-- 步骤二: 写入用户数据（sys_user，共 6 条）
-- 主键 id 显式指定为 1-6，供 todo_item.assignee_id 引用
-- 列顺序: id, username, nickname, email, phone, department, role, status, remark, version, created_at, updated_at
-- =============================================
INSERT INTO `sys_user`
    (`id`, `username`, `nickname`, `email`, `phone`, `department`, `role`, `status`, `remark`, `version`, `created_at`, `updated_at`)
VALUES
    -- 系统管理员：ADMIN / ACTIVE
    (1, 'admin',    '系统管理员', 'admin@taskhub.local',    '13800000001', '技术管理部', 'ADMIN',   'ACTIVE',   '拥有全部权限的内置管理员账号', 0, NOW(), NOW()),
    -- 项目经理：MANAGER / ACTIVE
    (2, 'zhangsan', '张三',       'zhangsan@taskhub.local', '13800000002', '研发部',     'MANAGER', 'ACTIVE',   '负责后端迭代排期',           0, NOW(), NOW()),
    -- 普通成员：MEMBER / ACTIVE
    (3, 'lisi',     '李四',       'lisi@taskhub.local',     '13800000003', '研发部',     'MEMBER',  'ACTIVE',   '前端开发',                   0, NOW(), NOW()),
    -- 普通成员：MEMBER / ACTIVE
    (4, 'wangwu',   '王五',       'wangwu@taskhub.local',   '13800000004', '设计部',     'MEMBER',  'ACTIVE',   'UI / 交互设计',              0, NOW(), NOW()),
    -- 普通成员：MEMBER / DISABLED（已离职停用）
    (5, 'zhaoliu',  '赵六',       'zhaoliu@taskhub.local',  '13800000005', '测试部',     'MEMBER',  'DISABLED', '已离职，账号停用',           0, NOW(), NOW()),
    -- 访客：GUEST / ACTIVE（无手机号，phone 为 NULL）
    (6, 'guest',    '访客账号',   'guest@taskhub.local',    NULL,          '运营部',     'GUEST',   'ACTIVE',   '只读演示账号',               0, NOW(), NOW());

-- =============================================
-- 步骤三: 写入待办数据（todo_item，共 13 条）
-- 主键 id 显式指定为 1-13；assignee_id 引用 sys_user.id（NULL 表示未分配）
-- due_date 使用 CURDATE() 相对偏移；completed_at 仅 DONE 状态写入
-- 列顺序: id, title, description, status, priority, due_date, completed_at, assignee_id, created_at, updated_at
-- =============================================
INSERT INTO `todo_item`
    (`id`, `title`, `description`, `status`, `priority`, `due_date`, `completed_at`, `assignee_id`, `created_at`, `updated_at`)
VALUES
    -- 【已逾期】URGENT / IN_PROGRESS，截止=今天-2，负责人 zhangsan(id=2)
    (1,  '修复登录接口偶发 500 问题',     '线上日志显示偶发空指针，需补充参数兜底与单元测试', 'IN_PROGRESS', 'URGENT', DATE_SUB(CURDATE(), INTERVAL 2 DAY), NULL, 2,    NOW(), NOW()),
    -- 【已逾期】HIGH / PENDING，截止=今天-1，负责人 lisi(id=3)
    (2,  '补充 API 接口文档',             '对齐 Swagger 分组与字段说明',                     'PENDING',     'HIGH',   DATE_SUB(CURDATE(), INTERVAL 1 DAY), NULL, 3,    NOW(), NOW()),
    -- 【今日到期】HIGH / PENDING，截止=今天，负责人 wangwu(id=4)
    (3,  '评审首页仪表盘设计稿',         '重点确认统计卡片的指标口径与配色方案',           'PENDING',     'HIGH',   CURDATE(),                            NULL, 4,    NOW(), NOW()),
    -- 【今日到期】MEDIUM / IN_PROGRESS，截止=今天，负责人 admin(id=1)，无描述
    (4,  '整理本周迭代待办清单',         NULL,                                              'IN_PROGRESS', 'MEDIUM', CURDATE(),                            NULL, 1,    NOW(), NOW()),
    -- HIGH / IN_PROGRESS，截止=今天+3，负责人 zhangsan(id=2)
    (5,  '实现待办事项分页查询接口',     '支持关键字、状态、优先级、负责人组合筛选',       'IN_PROGRESS', 'HIGH',   DATE_ADD(CURDATE(), INTERVAL 3 DAY),  NULL, 2,    NOW(), NOW()),
    -- MEDIUM / IN_PROGRESS，截止=今天+4，负责人 lisi(id=3)
    (6,  '封装 Axios 请求拦截器',         '统一处理响应解包、错误提示与登录态失效跳转',     'IN_PROGRESS', 'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 4 DAY),  NULL, 3,    NOW(), NOW()),
    -- MEDIUM / PENDING，截止=今天+2，负责人 lisi(id=3)
    (7,  '编写项目周报',                 '汇总本周研发进度、风险与下周计划，周五下班前提交', 'PENDING',   'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 2 DAY),  NULL, 3,    NOW(), NOW()),
    -- MEDIUM / PENDING，截止=今天+6，负责人 wangwu(id=4)
    (8,  '设计用户管理页面表格',         '包含筛选栏、批量操作与分页组件',                 'PENDING',     'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 6 DAY),  NULL, 4,    NOW(), NOW()),
    -- LOW / PENDING，截止=今天+9，负责人 zhangsan(id=2)，无描述
    (9,  '接入全局异常处理器',           NULL,                                              'PENDING',     'LOW',    DATE_ADD(CURDATE(), INTERVAL 9 DAY),  NULL, 2,    NOW(), NOW()),
    -- 【未分配负责人】MEDIUM / PENDING，截止=今天+14，assignee_id=NULL
    (10, '规划下一版本功能范围',         '尚未确定负责人，待周会讨论后分配',               'PENDING',     'MEDIUM', DATE_ADD(CURDATE(), INTERVAL 14 DAY), NULL, NULL, NOW(), NOW()),
    -- 【已完成】HIGH / DONE，截止=今天-5，完成时间=今天-6 的 17:30，负责人 admin(id=1)
    (11, '搭建 Spring Boot 项目骨架',    '完成分层结构、统一响应体与配置多环境',           'DONE',        'HIGH',   DATE_SUB(CURDATE(), INTERVAL 5 DAY),  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '17:30:00'), 1, NOW(), NOW()),
    -- 【已完成】HIGH / DONE，截止=今天-4，完成时间=今天-4 的 11:10，负责人 lisi(id=3)，无描述
    (12, '初始化 Vue 3 + TypeScript 前端工程', NULL,                                        'DONE',        'HIGH',   DATE_SUB(CURDATE(), INTERVAL 4 DAY),  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '11:10:00'), 3, NOW(), NOW()),
    -- 【已取消】LOW / CANCELLED，截止=今天+20，负责人 zhangsan(id=2)
    (13, '引入 Redis 做接口缓存',         '当前数据量较小，评估后决定暂缓',                 'CANCELLED',   'LOW',    DATE_ADD(CURDATE(), INTERVAL 20 DAY), NULL, 2,    NOW(), NOW());

-- 提交事务
COMMIT;

-- =============================================
-- 执行完成后校验（可选，取消注释后单独执行）
-- =============================================
-- 用户按角色/状态分布：
-- SELECT `role`, `status`, COUNT(*) AS cnt FROM `sys_user` GROUP BY `role`, `status`;
--
-- 待办按状态/优先级分布：
-- SELECT `status`, `priority`, COUNT(*) AS cnt FROM `todo_item` GROUP BY `status`, `priority`;
--
-- 逾期且未完成的待办（用于验证统计功能，应返回 id=1、2）：
-- SELECT `id`, `title`, `due_date`, `status` FROM `todo_item`
--  WHERE `due_date` < CURDATE() AND `status` IN ('PENDING', 'IN_PROGRESS');
--
-- 待办连同负责人姓名（验证外键关联，id=10 的负责人应为 NULL）：
-- SELECT t.`id`, t.`title`, t.`status`, u.`nickname` AS assignee
--   FROM `todo_item` t LEFT JOIN `sys_user` u ON t.`assignee_id` = u.`id` ORDER BY t.`id`;

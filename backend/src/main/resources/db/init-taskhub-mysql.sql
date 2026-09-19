-- =============================================
-- 脚本名称: init-taskhub-mysql.sql
-- 功能描述: TaskHub 任务管理平台 MySQL 数据库初始化脚本（生产规范）
--           依据 JPA 实体 User / TodoItem / BaseEntity 逆向生成，字段映射、长度、
--           非空约束、默认值、索引与外键均与实体注解严格对齐
-- 适用范围: MySQL 8.0+（兼容 5.7）；数据库版本 v1.0.0
-- 执行方式:
--   方式一: mysql -u root -p < init-taskhub-mysql.sql
--   方式二: 登录 MySQL 后 source /path/to/init-taskhub-mysql.sql
--   方式三: 配合 Flyway/Liquibase 做版本化迁移（推荐生产环境）
-- 执行顺序: 1（最先执行；sys_user 必须先于 todo_item 建表，因存在外键依赖）
-- 注意事项:
--   1. 脚本使用 CREATE TABLE IF NOT EXISTS，可重复执行且不会覆盖已有表
--   2. 若需清空重建，请按外键依赖逆序删除：先 DROP todo_item，再 DROP sys_user
--   3. 枚举列（role/status/priority）以字符串形式存储枚举名（JPA EnumType.STRING），
--      例如 'ADMIN'、'PENDING'，而非序号，请勿写入数字
--   4. created_at / updated_at 由 Spring Data JPA Auditing 在应用层自动维护，
--      此处的 DEFAULT / ON UPDATE 仅作为数据库层兜底，二者不冲突（应用显式写入优先）
--   5. 生产环境不写入演示数据（app.seed-data=false），本脚本仅含表结构 DDL
--
-- 作者: TaskHub
-- 创建时间: 2026-09-19
-- 修改记录:
--   2026-09-19 TaskHub 首次生成，覆盖 sys_user、todo_item 两张表
-- =============================================

-- =============================================
-- 数据字典（枚举值说明）
-- =============================================
-- 【UserStatus 账号状态】存储于 sys_user.status，类型 VARCHAR(20)
--   ACTIVE    - 正常启用（默认值）
--   DISABLED  - 已停用，禁止登录但历史数据保留
--   LOCKED    - 已锁定，如密码错误次数过多
--
-- 【UserRole 用户角色】存储于 sys_user.role，类型 VARCHAR(20)
--   ADMIN     - 系统管理员，拥有全部数据的管理权限
--   MANAGER   - 项目经理，可管理任务与分配负责人
--   MEMBER    - 普通成员，仅处理分配给自己的任务（默认值）
--   GUEST     - 访客，只读权限
--
-- 【TodoStatus 待办状态】存储于 todo_item.status，类型 VARCHAR(20)
--   流转路径: PENDING -> IN_PROGRESS -> DONE；PENDING/IN_PROGRESS 可 -> CANCELLED
--   PENDING     - 待处理，已创建但尚未开始（默认值，非终态）
--   IN_PROGRESS - 进行中，已开始处理但未完成（非终态）
--   DONE        - 已完成，任务正常结束（终态）
--   CANCELLED   - 已取消，任务被终止不再处理（终态）
--
-- 【TodoPriority 待办优先级】存储于 todo_item.priority，类型 VARCHAR(20)
--   LOW     - 低，权重 1，可延后处理
--   MEDIUM  - 中，权重 2，常规优先级（默认值）
--   HIGH    - 高，权重 3，需优先处理
--   URGENT  - 紧急，权重 4，立即处理
-- =============================================

-- 设置客户端字符集，确保中文注释与数据正确写入
SET NAMES utf8mb4;

-- 创建数据库（若不存在），统一使用 utf8mb4 字符集以支持完整 Unicode（含 emoji）
CREATE DATABASE IF NOT EXISTS `taskhub`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 切换到目标数据库
USE `taskhub`;


-- =============================================
-- 表名: sys_user
-- 功能描述: 用户信息表。表名使用 sys_user 而非 user，规避 USER 在多种数据库中的保留字冲突
-- 对应实体: com.example.taskhub.entity.User（继承 BaseEntity）
-- 维护人: TaskHub
-- 创建时间: 2026-09-19
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    /* 主键ID，数据库自增（对应 BaseEntity.id，GenerationType.IDENTITY） */
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，数据库自增',

    /* 登录用户名，全局唯一（uk_user_username） */
    `username` VARCHAR(50) NOT NULL COMMENT '登录用户名，全局唯一',

    /* 昵称 / 显示名，可为空 */
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称/显示名',

    /* 邮箱，全局唯一（uk_user_email） */
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱，全局唯一',

    /* 手机号，可为空 */
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',

    /* 所属部门，可为空 */
    `department` VARCHAR(50) DEFAULT NULL COMMENT '所属部门',

    /* 角色，字符串存储枚举名，默认 MEMBER */
    `role` VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
        COMMENT '角色: ADMIN-系统管理员 MANAGER-项目经理 MEMBER-普通成员(默认) GUEST-访客',

    /* 账号状态，字符串存储枚举名，默认 ACTIVE */
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        COMMENT '账号状态: ACTIVE-正常启用(默认) DISABLED-已停用 LOCKED-已锁定',

    /* 备注，可为空 */
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',

    /* 乐观锁版本号（对应 User.version，@Version），并发更新时防止后写覆盖先写 */
    `version` BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，每次更新自增',

    /* 创建时间，由 JPA Auditing @CreatedDate 维护，插入后不可修改 */
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    /* 最后更新时间，由 JPA Auditing @LastModifiedDate 维护 */
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    /* 主键 */
    PRIMARY KEY (`id`),

    /* 唯一索引: 用户名 */
    UNIQUE KEY `uk_user_username` (`username`),

    /* 唯一索引: 邮箱 */
    UNIQUE KEY `uk_user_email` (`email`),

    /* 普通索引: 账号状态，用于按状态筛选 */
    KEY `idx_user_status` (`status`),

    /* 普通索引: 角色，用于按角色筛选 */
    KEY `idx_user_role` (`role`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';


-- =============================================
-- 表名: todo_item
-- 功能描述: 待办事项表。与 sys_user 为多对一关系（一个待办最多指派一个负责人）
-- 对应实体: com.example.taskhub.entity.TodoItem（继承 BaseEntity）
-- 维护人: TaskHub
-- 创建时间: 2026-09-19
-- 说明: TodoItem 实体未声明 @Version 乐观锁字段，故本表不含 version 列，
--       与实体定义保持一致；如后续实体补充 @Version，需同步在此新增该列
-- =============================================
CREATE TABLE IF NOT EXISTS `todo_item` (
    /* 主键ID，数据库自增（对应 BaseEntity.id，GenerationType.IDENTITY） */
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID，数据库自增',

    /* 标题，必填 */
    `title` VARCHAR(120) NOT NULL COMMENT '标题，必填',

    /* 详细描述，可为空 */
    `description` VARCHAR(1000) DEFAULT NULL COMMENT '详细描述',

    /* 状态，字符串存储枚举名，默认 PENDING */
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT '状态: PENDING-待处理(默认) IN_PROGRESS-进行中 DONE-已完成 CANCELLED-已取消',

    /* 优先级，字符串存储枚举名，默认 MEDIUM */
    `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
        COMMENT '优先级: LOW-低(权重1) MEDIUM-中(权重2,默认) HIGH-高(权重3) URGENT-紧急(权重4)',

    /* 截止日期（对应 LocalDate），可为空 */
    `due_date` DATE DEFAULT NULL COMMENT '截止日期',

    /* 实际完成时间（对应 LocalDateTime），仅在状态流转到 DONE 时写入 */
    `completed_at` DATETIME DEFAULT NULL COMMENT '实际完成时间，仅在流转到 DONE 时写入',

    /* 负责人ID，关联 sys_user.id，可为空表示尚未分配 */
    `assignee_id` BIGINT DEFAULT NULL COMMENT '负责人ID，关联 sys_user.id，为空表示尚未分配',

    /* 创建时间，由 JPA Auditing @CreatedDate 维护，插入后不可修改 */
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    /* 最后更新时间，由 JPA Auditing @LastModifiedDate 维护 */
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    /* 主键 */
    PRIMARY KEY (`id`),

    /* 普通索引: 状态，用于按状态筛选与统计 */
    KEY `idx_todo_status` (`status`),

    /* 普通索引: 优先级，用于按优先级筛选 */
    KEY `idx_todo_priority` (`priority`),

    /* 普通索引: 截止日期，用于按到期时间排序与逾期查询 */
    KEY `idx_todo_due_date` (`due_date`),

    /* 普通索引: 负责人ID，用于按负责人查询 */
    KEY `idx_todo_assignee` (`assignee_id`),

    /* 外键: 负责人 -> sys_user.id
       负责人为可选关联，采用 ON DELETE SET NULL：删除用户时其待办自动置为"未分配"，
       避免因外键约束阻断用户删除；ON UPDATE CASCADE 保证主键变更时引用同步。
       注: 实体未指定删除规则，此为生产环境合理化选择，如需禁止删除可改为 RESTRICT */
    CONSTRAINT `fk_todo_assignee` FOREIGN KEY (`assignee_id`)
        REFERENCES `sys_user` (`id`)
        ON DELETE SET NULL
        ON UPDATE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办事项表';


-- =============================================
-- 执行完成后校验（可选）
-- =============================================
-- SHOW TABLES;                          -- 应包含 sys_user、todo_item
-- SHOW CREATE TABLE `sys_user`\G        -- 查看用户表结构与索引
-- SHOW CREATE TABLE `todo_item`\G       -- 查看待办表结构、索引与外键

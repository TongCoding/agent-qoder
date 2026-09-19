# TaskHub 全栈项目说明文档 [v1.0]

本文档旨在统一 TaskHub 项目的技术选型说明、目录结构约定、接口契约、设计取舍与运维手册，降低新成员上手成本，提升项目可维护性。适用于 Spring Boot 3.x 后端服务与 Vue 3 前端应用的开发、联调、测试、部署全流程。

---
## 目录导航

- [一、项目概述](#一项目概述)
  - [1.1 项目定位](#11-项目定位)
  - [1.2 技术栈清单](#12-技术栈清单)
  - [1.3 功能特性](#13-功能特性)
- [二、快速开始](#二快速开始)
  - [2.1 环境要求](#21-环境要求)
  - [2.2 后端服务启动](#22-后端服务启动)
  - [2.3 前端应用启动](#23-前端应用启动)
  - [2.4 接口连通性验证](#24-接口连通性验证)
- [三、项目目录结构](#三项目目录结构)
  - [3.1 整体结构](#31-整体结构)
  - [3.2 后端模块说明](#32-后端模块说明)
  - [3.3 前端模块说明](#33-前端模块说明)
- [四、接口文档](#四接口文档)
  - [4.1 待办事项接口](#41-待办事项接口)
  - [4.2 用户管理接口](#42-用户管理接口)
  - [4.3 统一响应契约](#43-统一响应契约)
  - [4.4 错误码清单](#44-错误码清单)
- [五、后端设计说明](#五后端设计说明)
  - [5.1 全局异常处理机制](#51-全局异常处理机制)
  - [5.2 分页与排序约定](#52-分页与排序约定)
  - [5.3 分层职责划分](#53-分层职责划分)
  - [5.4 N+1 查询规避](#54-n1-查询规避)
  - [5.5 演示数据初始化](#55-演示数据初始化)
  - [5.6 多环境配置](#56-多环境配置)
- [六、前端设计说明](#六前端设计说明)
  - [6.1 Axios 请求封装](#61-axios-请求封装)
  - [6.2 表格逻辑复用](#62-表格逻辑复用)
  - [6.3 枚举字典管理](#63-枚举字典管理)
  - [6.4 日期时间格式化](#64-日期时间格式化)
  - [6.5 查询参数清洗](#65-查询参数清洗)
  - [6.6 响应式布局断点](#66-响应式布局断点)
  - [6.7 路由与菜单单一数据源](#67-路由与菜单单一数据源)
- [七、测试说明](#七测试说明)
  - [7.1 后端测试](#71-后端测试)
  - [7.2 前端类型检查与构建](#72-前端类型检查与构建)
- [八、生产部署](#八生产部署)
  - [8.1 后端打包与启动](#81-后端打包与启动)
  - [8.2 前端打包](#82-前端打包)
  - [8.3 Nginx 配置](#83-nginx-配置)
- [九、常见问题](#九常见问题)
  - [9.1 启动与连通性问题](#91-启动与连通性问题)
  - [9.2 数据与配置问题](#92-数据与配置问题)
  - [9.3 扩展与演进问题](#93-扩展与演进问题)
- [十、维护记录](#十维护记录)
  - [10.1 文档信息与版本历史](#101-文档信息与版本历史)
  - [10.2 交付物核对清单](#102-交付物核对清单)

## 一、项目概述

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[1.1 项目定位](#11-项目定位) | [1.2 技术栈清单](#12-技术栈清单) | [1.3 功能特性](#13-功能特性)

### 1.1 项目定位

> [⬆ 返回本章导航](#一项目概述) | [⬆ 返回目录导航](#目录导航)

```yaml
# ==============================================
# TaskHub 项目基本信息
# 版本：v1.0
# 更新：2026-09-13
# ==============================================

project:
  name: TaskHub                        # 项目名称
  type: 前后端分离全栈示例              # 项目类型
  modules:                             # 业务模块
    - todo                             # 待办事项管理
    - user                             # 用户管理

# ==============================================
# 定位说明
# ==============================================
positioning:
  goal: 工程规范示例                    # 核心目标
  focus:                               # 重点关注
    - 分层结构与依赖方向                 # 关注点一
    - 统一响应契约与错误码体系           # 关注点二
    - 全局异常兜底                       # 关注点三
    - 集成测试覆盖                       # 关注点四
    - 可复现的构建与部署配置             # 关注点五
  not-focus: 业务复杂度                 # 刻意不追求的部分
```

本项目是一个**可直接启动、可直接测试**的全栈脚手架，后端使用 Spring Boot 3 + Java 17，前端使用 Vue 3 + TypeScript + Element Plus。

业务上同时实现了「待办事项」与「用户管理」两个模块，覆盖后台管理系统中最常见的**分页筛选、服务端排序、一对多关联、状态流转、聚合统计、参数校验与全局异常处理**等场景。

> 项目定位为**工程规范示例**：不追求业务复杂度，而是把分层结构、统一契约、错误处理、测试与构建配置这些「容易被省略但决定项目可维护性」的部分做扎实。

### 1.2 技术栈清单

> [⬆ 返回本章导航](#一项目概述) | [⬆ 返回目录导航](#目录导航)

#### 后端技术栈

| 分类 | 选型 | 版本 | 说明 |
| --- | --- | --- | --- |
| 语言 | Java | 17 | LTS 版本，启用 Records、`switch` 表达式等特性 |
| 框架 | Spring Boot | 3.5.9 | Web MVC + Spring Data JPA |
| 数据库 | H2 | 2.3.x（随 Boot 管理） | 内存模式，启动即用，无需安装 |
| ORM | Hibernate | 6.x（随 Boot 管理） | Criteria API / Specification 实现动态查询 |
| 接口文档 | springdoc-openapi | 2.8.17 | 生成 OpenAPI 3.1 文档并内嵌 Swagger UI |
| 参数校验 | Jakarta Bean Validation | 3.0 | `@Valid` + 注解约束 |
| 监控 | Spring Boot Actuator | 随 Boot | 健康检查端点 |
| 简化代码 | Lombok | 随 Boot | `@Data` / `@Builder` / `@RequiredArgsConstructor` |
| 测试 | JUnit 5 + MockMvc + Mockito + AssertJ | 随 Boot | 共 58 个用例 |
| 构建 | Maven | 3.8+ | `spring-boot-maven-plugin` 打可执行 jar |

#### 前端技术栈

| 分类 | 选型 | 版本 | 说明 |
| --- | --- | --- | --- |
| 框架 | Vue | 3.5.x | 全量使用 `<script setup>` Composition API |
| 语言 | TypeScript | 5.9.x | `strict` 模式 + `noUnusedLocals` / `noUnusedParameters` |
| 构建 | Vite | 8.3.x | 开发代理 + 生产打包与手动分包 |
| UI | Element Plus | 2.14.x | 中文语言包 + 全量图标注册 |
| HTTP | Axios | 1.20.x | 统一实例、拦截器、类型安全请求封装 |
| 路由 | Vue Router | 4.6.x | History 模式 + 路由懒加载 |
| 状态 | Pinia | 3.0.x | 仅存放跨页面共享的 UI 状态 |
| 样式 | Sass | 1.104.x | CSS 自定义属性做设计令牌 + 组件内 scoped 嵌套 |
| 类型检查 | vue-tsc | 3.3.x | 构建前全量类型校验 |

### 1.3 功能特性

> [⬆ 返回本章导航](#一项目概述) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 功能特性清单
# ==============================================

待办事项模块
  ├─ 分页查询          支持 8 个筛选维度组合
  ├─ 服务端排序        后端 8 字段白名单，前端开放 5 列可排序
  ├─ 增删改查          含批量删除（ids 逗号分隔）
  ├─ 状态流转          4 态可逆流转，含已完成重开、已取消恢复
  ├─ 聚合统计          状态分布、优先级分布、逾期数、完成率
  └─ 详情抽屉          行内快速查看，无需跳转

用户管理模块
  ├─ 分页查询          关键字 / 角色 / 状态 / 部门筛选
  ├─ 增删改查          用户名创建后不可修改
  ├─ 账号状态变更      启用 / 禁用 / 锁定
  ├─ 唯一性校验        用户名冲突 40901、邮箱冲突 40902
  ├─ 关联保护          名下有待办时禁止删除（40903）
  └─ 下拉选项接口      仅返回 ACTIVE 用户，供待办负责人选择

仪表盘
  ├─ 4 张统计卡片      待办总数、进行中、逾期、用户总数
  ├─ 状态分布          进度条可视化 + 百分比
  ├─ 优先级分布        同上
  ├─ 角色分布          同上
  └─ 最近待办表格      按创建时间倒序取前 5 条

工程能力
  ├─ 全局异常处理      Controller 内零 try-catch
  ├─ 统一响应契约      成功与异常同构
  ├─ 字段级校验回显    服务端错误直接标注到表单项
  ├─ 集成测试          58 个用例覆盖正常与异常分支
  ├─ 响应式布局        4 级断点，移动端抽屉导航
  └─ 接口文档          Swagger UI 在线调试
```

## 二、快速开始

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[2.1 环境要求](#21-环境要求) | [2.2 后端服务启动](#22-后端服务启动) | [2.3 前端应用启动](#23-前端应用启动) | [2.4 接口连通性验证](#24-接口连通性验证)

### 2.1 环境要求

> [⬆ 返回本章导航](#二快速开始) | [⬆ 返回目录导航](#目录导航)

| 工具 | 最低版本 | 检查命令 | 备注 |
| --- | --- | --- | --- |
| JDK | 17 | `java -version` | 必须为 17 或以上，低于 17 无法编译 |
| Maven | 3.8 | `mvn -v` | 建议配置国内镜像加速依赖下载 |
| Node.js | 20.19 | `node -v` | Vite 8 不支持低于 20.19 的版本 |
| npm | 10 | `npm -v` | 随 Node.js 一同安装 |

> 前端 `package.json` 中通过 `engines` 字段声明了 Node 版本要求，版本不满足时 `npm install` 会给出告警。

### 2.2 后端服务启动

> [⬆ 返回本章导航](#二快速开始) | [⬆ 返回目录导航](#目录导航)

```shell
#!/bin/bash
# ============================================================================
# 脚本名称: start-backend.sh（示例命令，可直接复制执行）
# 功能描述: 以开发模式启动 TaskHub 后端服务
# 使用场景: 本地开发、接口联调、功能演示
# 使用方法:
#   cd backend && mvn spring-boot:run
# 前置条件:
#   1. JDK 17 已安装并配置 JAVA_HOME
#   2. Maven 3.8+ 已安装
#   3. 8080 端口未被占用
# 注意事项:
#   - 默认激活 dev profile，使用 H2 内存数据库
#   - 启动时自动灌入演示数据（6 个用户 + 13 条待办）
#   - 内存库在进程退出后数据全部丢失，属刻意设计
# 作者: 刘武贵
# 创建时间: 2026-09-13
# ============================================================================

# 进入后端工程目录
cd backend

# 开发模式启动（默认 dev profile：H2 内存库 + 自动灌入演示数据）
mvn spring-boot:run
```

看到下面两行即表示启动成功：

```text
Started TaskHubApplication in x.xxx seconds
演示数据初始化完成 | 用户 6 条 | 待办 13 条
```

后端默认监听 **http://localhost:8080**，可访问以下端点：

| 地址 | 说明 |
| --- | --- |
| http://localhost:8080/swagger-ui.html | Swagger UI 接口文档（可直接在线调试） |
| http://localhost:8080/v3/api-docs | OpenAPI 3.1 JSON 描述文件 |
| http://localhost:8080/actuator/health | 健康检查，正常返回 `{"status":"UP"}` |
| http://localhost:8080/h2-console | H2 数据库控制台 |

```text
# ==============================================
# H2 控制台连接参数
# ==============================================
JDBC URL: jdbc:h2:mem:taskhub      # 内存库连接串
User Name: sa                      # 默认用户名
Password:                          # 留空，无密码
```

也可以使用打包好的 jar 启动：

```shell
# ------------------------------------------
# 方式二：jar 包启动
# ------------------------------------------
cd backend
mvn clean package                                    # 打包（会先执行全部测试）
java -jar target/taskhub-backend.jar                 # 启动可执行 jar
```

### 2.3 前端应用启动

> [⬆ 返回本章导航](#二快速开始) | [⬆ 返回目录导航](#目录导航)

```shell
#!/bin/bash
# ============================================================================
# 脚本名称: start-frontend.sh（示例命令，可直接复制执行）
# 功能描述: 启动 TaskHub 前端开发服务器
# 使用场景: 本地开发、页面调试
# 使用方法:
#   cd frontend && npm install && npm run dev
# 前置条件:
#   1. Node.js 20.19+ 已安装
#   2. 后端服务已在 8080 端口启动（否则页面数据为空）
#   3. 5173 端口未被占用
# 注意事项:
#   - vite.config.ts 中设置了 strictPort: true
#     端口冲突时会直接报错退出，而不是静默换端口
#   - 修改 .env.development 后必须重启 dev server（env 变更不会热更新）
# 作者: 刘武贵
# 创建时间: 2026-09-13
# ============================================================================

# 进入前端工程目录
cd frontend

# 首次运行需要安装依赖（约 118 个包）
npm install

# 启动开发服务器
npm run dev
```

前端默认监听 **http://localhost:5173**，浏览器打开即可看到仪表盘。

```ts
// ==============================================
// 开发服务器配置（frontend/vite.config.ts 真实片段）
// ==============================================
// loadEnv 第三个参数传空串，表示不按 VITE_ 前缀过滤，
// 方便读取 VITE_PROXY_TARGET 等自定义变量
const env = loadEnv(mode, process.cwd(), '')
const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

server: {
  host: '0.0.0.0',                    // 监听所有网卡，便于局域网内手机调试
  port: Number(env.VITE_PORT) || 5173, // 端口可由 env 覆盖
  // 端口被占用时直接报错退出，而不是静默切换到其他端口，
  // 避免前后端代理配置错位
  strictPort: true,
  open: false,                        // 不自动打开浏览器
  proxy: {
    '/api': {                         // 匹配前缀
      target: proxyTarget,            // 后端地址（来自 env）
      changeOrigin: true,             // 改写 Origin 头，规避后端 CORS 校验
      // 后端接口本身就以 /api 开头，因此不做路径重写（无 rewrite）
    },
  },
},
```

开发环境下，前端所有 `/api/**` 请求都会由 Vite 代理转发到 `http://localhost:8080`，因此**不存在浏览器跨域问题**。后端地址可通过 `frontend/.env.development` 中的 `VITE_PROXY_TARGET` 修改。

### 2.4 接口连通性验证

> [⬆ 返回本章导航](#二快速开始) | [⬆ 返回目录导航](#目录导航)

```shell
#!/bin/bash
# ============================================================================
# 脚本名称: verify-api.sh（示例命令，可逐条复制执行）
# 功能描述: 验证后端接口是否正常提供服务
# 使用场景: 服务启动后自检、前后端联调排障
# 前置条件:
#   1. 后端服务已在 8080 端口启动
#   2. curl 已安装
# 注意事项:
#   - 所有响应均为统一结构 { code, message, data, timestamp }
#   - code = 0 表示成功，非 0 为业务错误码
# 作者: 刘武贵
# 创建时间: 2026-09-13
# ============================================================================

# ------------------------------------------
# 1. 健康检查（最快速的存活确认）
# ------------------------------------------
curl "http://localhost:8080/actuator/health"

# ------------------------------------------
# 2. 待办分页查询（注意 page 从 1 开始）
# ------------------------------------------
curl "http://localhost:8080/api/v1/todos?page=1&size=3"

# ------------------------------------------
# 3. 关键字搜索（演示数据中已预置「编写项目周报」）
# ------------------------------------------
curl "http://localhost:8080/api/v1/todos?keyword=周报"

# ------------------------------------------
# 4. 待办聚合统计（仪表盘数据来源）
# ------------------------------------------
curl "http://localhost:8080/api/v1/todos/statistics"

# ------------------------------------------
# 5. 用户下拉选项（仅返回 ACTIVE 用户）
# ------------------------------------------
curl "http://localhost:8080/api/v1/users/options"

# ------------------------------------------
# 6. 创建一条待办（成功返回 HTTP 201）
# ------------------------------------------
curl -X POST "http://localhost:8080/api/v1/todos" \
  -H 'Content-Type: application/json' \
  -d '{"title":"测试任务","priority":"HIGH","dueDate":"2026-12-31","assigneeId":2}'

# ------------------------------------------
# 7. 触发参数校验失败（返回 code=40001 与字段级明细）
# ------------------------------------------
curl -X POST "http://localhost:8080/api/v1/todos" \
  -H 'Content-Type: application/json' \
  -d '{"title":""}'

# ------------------------------------------
# 8. 触发资源不存在（返回 code=40401）
# ------------------------------------------
curl "http://localhost:8080/api/v1/todos/999999"
```

第 7 条命令的预期响应：

```json
{
  "code": 40001,
  "message": "标题不能为空",
  "data": [
    { "field": "title", "message": "标题不能为空", "rejectedValue": "" }
  ],
  "timestamp": "2026-09-13T17:47:11.225090"
}
```

## 三、项目目录结构

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[3.1 整体结构](#31-整体结构) | [3.2 后端模块说明](#32-后端模块说明) | [3.3 前端模块说明](#33-前端模块说明)

### 3.1 整体结构

> [⬆ 返回本章导航](#三项目目录结构) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# TaskHub 顶层目录结构
# ==============================================
agent-qoder/
├── backend/                     # Spring Boot 后端工程（53 个 Java 文件）
├── frontend/                    # Vue 3 前端工程（27 个源文件）
├── .editorconfig                # 编辑器统一格式配置
├── .gitignore                   # 版本忽略规则
├── README.md                    # 项目简介文档
├── TaskHub.md                   # 本文档
└── 注释规范.md                   # 团队注释规范指南
```

### 3.2 后端模块说明

> [⬆ 返回本章导航](#三项目目录结构) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# backend/ 目录结构
# ==============================================
backend/
├── pom.xml                                       # Maven 构建与依赖声明
└── src
    ├── main
    │   ├── java/com/example/taskhub
    │   │   ├── TaskHubApplication.java            # Spring Boot 启动类
    │   │   │
    │   │   ├── common/                            # 通用契约层
    │   │   │   ├── ApiResponse.java               #   统一响应体（4 字段）
    │   │   │   ├── ErrorCode.java                 #   业务错误码枚举
    │   │   │   ├── PageQuery.java                 #   分页请求基类（对外 1-based）
    │   │   │   └── PageResult.java                #   分页响应体
    │   │   │
    │   │   ├── config/                            # 配置层
    │   │   │   ├── AppProperties.java             #   app.* 自定义配置绑定
    │   │   │   ├── DataInitializer.java           #   演示数据灌入（ApplicationRunner）
    │   │   │   ├── OpenApiConfig.java             #   Swagger 文档元信息
    │   │   │   └── WebConfig.java                 #   CORS / Jackson 等 Web 配置
    │   │   │
    │   │   ├── controller/                        # 接口层（只做参数绑定与响应包装）
    │   │   │   ├── TodoController.java            #   待办事项接口，8 个端点
    │   │   │   └── UserController.java            #   用户管理接口，8 个端点
    │   │   │
    │   │   ├── dto/                               # 数据传输对象
    │   │   │   ├── todo/                          #   待办相关（6 个 Record）
    │   │   │   │   ├── TodoCreateRequest.java     #     创建请求
    │   │   │   │   ├── TodoUpdateRequest.java     #     更新请求
    │   │   │   │   ├── TodoQueryRequest.java      #     查询条件（含排序白名单）
    │   │   │   │   ├── TodoStatusUpdateRequest.java #   状态流转请求
    │   │   │   │   ├── TodoResponse.java          #     列表/详情响应
    │   │   │   │   └── TodoStatisticsResponse.java #    聚合统计响应
    │   │   │   └── user/                          #   用户相关（7 个 Record）
    │   │   │       ├── UserCreateRequest.java     #     创建请求
    │   │   │       ├── UserUpdateRequest.java     #     更新请求
    │   │   │       ├── UserQueryRequest.java      #     查询条件（含排序白名单）
    │   │   │       ├── UserStatusUpdateRequest.java #   状态变更请求
    │   │   │       ├── UserResponse.java          #     列表/详情响应
    │   │   │       ├── UserOptionResponse.java    #     下拉选项响应
    │   │   │       └── UserStatisticsResponse.java #    聚合统计响应
    │   │   │
    │   │   ├── entity/                            # JPA 实体层
    │   │   │   ├── BaseEntity.java                #   公共字段 id / createdAt / updatedAt
    │   │   │   ├── TodoItem.java                  #   待办实体，表名 todo_item
    │   │   │   └── User.java                      #   用户实体，表名 sys_user
    │   │   │
    │   │   ├── enums/                             # 业务枚举
    │   │   │   ├── TodoStatus.java                #   待处理/进行中/已完成/已取消
    │   │   │   ├── TodoPriority.java              #   低/中/高/紧急
    │   │   │   ├── UserRole.java                  #   管理员/开发/测试/产品/设计
    │   │   │   └── UserStatus.java                #   启用/禁用/锁定
    │   │   │
    │   │   ├── exception/                         # 异常处理层
    │   │   │   ├── BusinessException.java         #   业务异常（携带 ErrorCode）
    │   │   │   ├── FieldValidationError.java      #   字段级校验错误明细
    │   │   │   └── GlobalExceptionHandler.java    #   全局异常兜底（@RestControllerAdvice）
    │   │   │
    │   │   ├── mapper/                            # Entity ↔ DTO 手动转换
    │   │   │   ├── TodoMapper.java
    │   │   │   └── UserMapper.java
    │   │   │
    │   │   ├── repository/                        # 数据访问层
    │   │   │   ├── TodoRepository.java            #   Spring Data JPA
    │   │   │   ├── UserRepository.java
    │   │   │   ├── projection/                    #   接口投影（聚合统计专用）
    │   │   │   │   ├── AssigneeTodoCount.java     #     按负责人统计待办数
    │   │   │   │   ├── TodoStatusCount.java       #     按状态统计
    │   │   │   │   ├── TodoPriorityCount.java     #     按优先级统计
    │   │   │   │   ├── UserRoleCount.java         #     按角色统计
    │   │   │   │   └── UserStatusCount.java       #     按账号状态统计
    │   │   │   └── specification/                 #   动态查询条件
    │   │   │       ├── TodoSpecifications.java    #     待办组合筛选 + LEFT JOIN FETCH
    │   │   │       └── UserSpecifications.java    #     用户组合筛选
    │   │   │
    │   │   └── service/                           # 业务逻辑层
    │   │       ├── TodoService.java               #   待办服务接口
    │   │       ├── UserService.java               #   用户服务接口
    │   │       └── impl/
    │   │           ├── TodoServiceImpl.java       #   待办服务实现
    │   │           └── UserServiceImpl.java       #   用户服务实现
    │   │
    │   └── resources/                             # 配置资源
    │       ├── application.yml                    #   公共配置（所有环境共享）
    │       ├── application-dev.yml                #   开发环境（H2 内存 + SQL 日志）
    │       └── application-prod.yml               #   生产环境（收紧日志与 CORS）
    │
    └── test
        ├── java/com/example/taskhub/
        │   ├── TaskHubApplicationTests.java        # 上下文加载测试（2 例）
        │   ├── controller/                         # MockMvc 集成测试
        │   │   ├── TodoApiIntegrationTest.java     #   待办接口（27 例）
        │   │   └── UserApiIntegrationTest.java     #   用户接口（18 例）
        │   └── service/impl/
        │       └── TodoServiceImplTest.java        # Mockito 单元测试（11 例）
        └── resources/
            └── application-test.yml                # 测试环境配置（关闭演示数据）
```

### 3.3 前端模块说明

> [⬆ 返回本章导航](#三项目目录结构) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# frontend/ 目录结构
# ==============================================
frontend/
├── index.html                            # HTML 入口模板
├── package.json                          # 依赖与脚本声明
├── package-lock.json                     # 依赖锁定（保证可复现构建）
├── tsconfig.json                         # TypeScript 编译配置（strict）
├── vite.config.ts                        # Vite 构建配置（代理 + 分包）
├── .env.development                      # 开发环境变量
├── .env.production                       # 生产环境变量
├── public/
│   └── favicon.svg                       # 站点图标
└── src
    ├── main.ts                           # 应用入口：注册 Element Plus / Pinia / Router
    ├── App.vue                           # 根组件
    ├── env.d.ts                          # import.meta.env 与 .vue 模块类型声明
    │
    ├── api/                              # 接口请求层
    │   ├── request.ts                    #   Axios 实例 + 拦截器 + http 类型安全封装
    │   ├── todo.ts                       #   待办接口定义
    │   └── user.ts                       #   用户接口定义
    │
    ├── types/                            # TypeScript 类型定义
    │   ├── api.ts                        #   ApiResponse / PageResult / ApiError
    │   ├── todo.ts                       #   待办相关类型与枚举
    │   └── user.ts                       #   用户相关类型与枚举
    │
    ├── composables/                      # 组合式函数（逻辑复用）
    │   ├── useTable.ts                   #   分页表格通用逻辑（含请求竞态处理）
    │   ├── useUserOptions.ts             #   负责人下拉数据加载与缓存
    │   └── useMediaQuery.ts              #   响应式断点判断（基于 matchMedia）
    │
    ├── stores/
    │   └── app.ts                        # Pinia：侧边栏折叠状态（持久化到 localStorage）
    │
    ├── router/
    │   └── index.ts                      # 路由表（导出 mainRoutes 供菜单派生）
    │
    ├── styles/
    │   └── index.scss                    # 设计令牌 + 全局工具类 + 路由过渡动画
    │
    ├── utils/                            # 纯函数工具
    │   ├── dict.ts                       #   枚举字典（中文 label / tag 颜色 / 状态流转）
    │   ├── format.ts                     #   日期时间格式化（纯字符串解析，无时区漂移）
    │   └── params.ts                     #   查询参数清洗与 ID 拼接
    │
    ├── layouts/                          # 布局组件
    │   ├── DefaultLayout.vue             #   侧边栏 + 顶栏 + 内容区（含移动端抽屉）
    │   └── SideMenu.vue                  #   菜单（桌面侧栏与移动抽屉共用）
    │
    ├── components/                       # 业务组件
    │   ├── common/
    │   │   └── StatCard.vue              #     统计卡片（支持点击跳转）
    │   ├── todo/
    │   │   └── TodoFormDialog.vue        #     待办新建/编辑弹窗（含服务端错误回填）
    │   └── user/
    │       └── UserFormDialog.vue        #     用户新建/编辑弹窗
    │
    └── views/                            # 页面视图
        ├── DashboardView.vue             #   仪表盘（聚合统计 + 分布图 + 最近待办）
        ├── TodoListView.vue              #   待办列表（筛选/排序/分页/流转/批量删除）
        ├── UserListView.vue              #   用户列表（筛选/状态变更/关联保护提示）
        └── NotFoundView.vue              #   404 页面
```


## 四、接口文档

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[4.1 待办事项接口](#41-待办事项接口) | [4.2 用户管理接口](#42-用户管理接口) | [4.3 统一响应契约](#43-统一响应契约) | [4.4 错误码清单](#44-错误码清单)

启动后端后打开 **http://localhost:8080/swagger-ui.html**，可直接在线调试全部接口。
前端顶栏右侧也提供了「接口文档」快捷入口（地址由 `VITE_SWAGGER_URL` 配置，生产环境默认为空即隐藏）。

### 4.1 待办事项接口

> [⬆ 返回本章导航](#四接口文档) | [⬆ 返回目录导航](#目录导航)

```yaml
# ==============================================
# 待办事项接口清单
# 基础路径: /api/v1/todos
# 控制器: TodoController
# 端点数量: 8
# ==============================================
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/todos` | 分页查询，支持关键字 / 状态 / 优先级 / 负责人 / 未分配 / 逾期 / 截止日期区间组合筛选 |
| GET | `/api/v1/todos/{id}` | 查询详情 |
| GET | `/api/v1/todos/statistics` | 聚合统计（仪表盘一次请求拿全部指标） |
| POST | `/api/v1/todos` | 创建，成功返回 HTTP 201 |
| PUT | `/api/v1/todos/{id}` | 全量更新 |
| PATCH | `/api/v1/todos/{id}/status` | 仅流转状态，转为 `DONE` 时自动写入 `completedAt` |
| DELETE | `/api/v1/todos/{id}` | 删除单条 |
| DELETE | `/api/v1/todos?ids=1,2,3` | 批量删除，存在无效 ID 则整体失败 |

```yaml
# ==============================================
# 查询参数说明（GET /api/v1/todos）
# ==============================================
query-params:
  page: 1                        # 页码，从 1 开始（非 0）
  size: 10                       # 每页条数，上限 100，超出被截断
  sortBy: createdAt              # 排序字段，须在白名单内
  sortDirection: desc            # 排序方向：asc / desc
  keyword: 周报                  # 标题或描述模糊匹配
  status: PENDING                # 待办状态筛选
  priority: HIGH                 # 优先级筛选
  assigneeId: 2                  # 负责人 ID 筛选
  unassigned: true               # 仅查询未分配负责人的待办
  overdue: true                  # 仅查询已逾期未完成的待办
  dueDateFrom: 2026-09-01        # 截止日期区间起（含）
  dueDateTo: 2026-09-30          # 截止日期区间止（含）

# ==============================================
# 排序字段白名单
# 说明: 未命中白名单时静默回退到默认排序字段，不抛异常
# ==============================================
sortable-fields:
  - id                           # 主键
  - title                        # 标题
  - status                       # 状态
  - priority                     # 优先级
  - dueDate                      # 截止日期
  - completedAt                  # 完成时间
  - createdAt                    # 创建时间（默认排序字段）
  - updatedAt                    # 更新时间
```

```yaml
# ==============================================
# 响应字段说明（TodoResponse，共 11 个字段）
# ==============================================
response-fields:
  id: 13                         # 待办 ID
  title: 引入 Redis 做接口缓存     # 标题
  description: 当前数据量较小...    # 详细描述（为 null 时被省略）
  status: CANCELLED              # 状态枚举
  priority: LOW                  # 优先级枚举
  dueDate: 2026-10-03            # 截止日期（LocalDate，不含时间）
  completedAt: 2026-09-13T10:00:00  # 完成时间（未完成时该键不出现）
  overdue: false                 # 是否逾期（服务端计算字段，非数据库列）
  assignee:                      # 负责人（精简嵌套对象，未分配时该键不出现）
    id: 2                        #   用户 ID
    username: zhangsan           #   登录用户名
    nickname: 张三                #   昵称
  createdAt: 2026-09-13T18:18:13.703276   # 创建时间
  updatedAt: 2026-09-13T18:18:13.703276   # 更新时间

# ==============================================
# 设计要点
# ==============================================
design-notes:
  assignee-projection: 只嵌套 id / username / nickname 三个字段
                       而非完整 UserResponse，避免报文冗余与循环引用
  overdue-computed: 由服务端根据 dueDate 与当前状态实时计算
                    前端无需重复实现逾期判断逻辑
  left-join-fetch: 列表查询通过 LEFT JOIN FETCH 一次性带出负责人
                   详见 5.4 N+1 查询规避
```

聚合统计响应（`GET /api/v1/todos/statistics`）实测样例：

```json
{
  "total": 13,
  "statusCounts": { "PENDING": 6, "IN_PROGRESS": 4, "DONE": 2, "CANCELLED": 1 },
  "priorityCounts": { "LOW": 2, "MEDIUM": 5, "HIGH": 5, "URGENT": 1 },
  "overdueCount": 2,
  "dueTodayCount": 2,
  "unassignedCount": 1,
  "completionRate": 15.38,
  "recentTodos": [ ]
}
```

```yaml
# ==============================================
# 统计字段说明（TodoStatisticsResponse，共 8 个字段）
# ==============================================
statistics-fields:
  total: 13                      # 待办总数
  statusCounts: {}               # 状态分布。key: TodoStatus 枚举名，value: 数量
  priorityCounts: {}             # 优先级分布。key: TodoPriority 枚举名，value: 数量
  overdueCount: 2                # 逾期数（仅统计未完成状态）
  dueTodayCount: 2               # 今日到期数（仅统计未完成状态）
  unassignedCount: 1             # 未分配负责人的数量
  completionRate: 15.38          # 完成率百分比（仅 DONE 计入分子，保留两位小数）
  recentTodos: []                # 最近创建的待办，默认取前 5 条（含负责人）

# ==============================================
# Map 类型字段的 key / value 约定
# ==============================================
map-convention:
  statusCounts:
    key: TodoStatus 枚举名（PENDING / IN_PROGRESS / DONE / CANCELLED）
    value: 该状态下的待办数量（Long）
  priorityCounts:
    key: TodoPriority 枚举名（LOW / MEDIUM / HIGH / URGENT）
    value: 该优先级下的待办数量（Long）

# ==============================================
# 实现细节：EnumMap + 零值预填
# ==============================================
implementation:
  map-type: EnumMap              # 而非 HashMap
  reason-1: 迭代顺序恒为枚举声明顺序，前端渲染顺序稳定可预期
  reason-2: EnumMap 底层为数组，读写开销低于 HashMap
  zero-filling: |
    先遍历 TodoStatus.values() 把每个枚举预填为 0L，
    再用 GROUP BY 查询结果覆盖有数据的项。
  benefit: |
    数量为 0 的状态【一定】出现在 Map 中，
    前端遍历字典渲染分布图时无需做 undefined 兜底，
    也不会因为某个状态恰好无数据而导致图例缺项。
```

### 4.2 用户管理接口

> [⬆ 返回本章导航](#四接口文档) | [⬆ 返回目录导航](#目录导航)

```yaml
# ==============================================
# 用户管理接口清单
# 基础路径: /api/v1/users
# 控制器: UserController
# 端点数量: 8
# ==============================================
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/users` | 分页查询，支持关键字 / 角色 / 状态 / 部门筛选 |
| GET | `/api/v1/users/{id}` | 查询详情 |
| GET | `/api/v1/users/options` | 下拉选项（仅 `ACTIVE` 用户），用于待办的负责人选择器 |
| GET | `/api/v1/users/statistics` | 总数 + 角色分布 + 状态分布 |
| POST | `/api/v1/users` | 创建，成功返回 HTTP 201 |
| PUT | `/api/v1/users/{id}` | 全量更新（用户名不可改） |
| PATCH | `/api/v1/users/{id}/status` | 变更账号状态 |
| DELETE | `/api/v1/users/{id}` | 删除；名下仍有待办时返回 `40903` |

```yaml
# ==============================================
# 查询参数说明（GET /api/v1/users）
# ==============================================
query-params:
  page: 1                        # 页码，从 1 开始
  size: 10                       # 每页条数，上限 100
  sortBy: createdAt              # 排序字段，须在白名单内
  sortDirection: desc            # 排序方向：asc / desc
  keyword: 张                    # 模糊匹配 username / nickname / email（三字段 OR，大小写不敏感）
  role: MEMBER                   # 角色筛选：ADMIN / MANAGER / MEMBER / GUEST
  status: ACTIVE                 # 账号状态：ACTIVE / DISABLED / LOCKED
  department: 研发部              # 部门精确匹配（自动 trim）

# ==============================================
# 响应字段说明（UserResponse，共 12 个字段）
# 示例值取自 GET /api/v1/users/3 的真实响应
# ==============================================
response-fields:
  id: 3                          # 用户 ID
  username: lisi                 # 登录用户名（创建后不可修改）
  nickname: 李四                 # 昵称
  email: lisi@taskhub.local      # 邮箱（唯一约束）
  phone: '13800000003'           # 手机号
  department: 研发部              # 所属部门
  role: MEMBER                   # 角色枚举
  status: ACTIVE                 # 账号状态枚举
  remark: 前端开发                # 备注（为 null 时不出现在 JSON 中）
  todoCount: 4                   # 名下待办数量（聚合查询填充，避免 N+1）
  createdAt: 2026-09-13T18:18:13.691036  # 创建时间（ISO-8601，含微秒）
  updatedAt: 2026-09-13T18:18:13.691036  # 更新时间（ISO-8601，含微秒）
```

真实响应报文（原始为单行，此处已格式化便于阅读）：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "id": 3,
    "username": "lisi",
    "nickname": "李四",
    "email": "lisi@taskhub.local",
    "phone": "13800000003",
    "department": "研发部",
    "role": "MEMBER",
    "status": "ACTIVE",
    "remark": "前端开发",
    "todoCount": 4,
    "createdAt": "2026-09-13T18:18:13.691036",
    "updatedAt": "2026-09-13T18:18:13.691036"
  },
  "timestamp": "2026-09-13T18:50:33.81533"
}
```

> `todoCount` 是响应专用字段，数据库中并不存在该列，由 `UserServiceImpl.loadTodoCounts()` 通过一次 `GROUP BY` 聚合查询填充（详见 [5.4 N+1 查询规避](#54-n1-查询规避)）。

### 4.3 统一响应契约

> [⬆ 返回本章导航](#四接口文档) | [⬆ 返回目录导航](#目录导航)

所有接口（**包括异常响应**）都返回同一结构，前端只需一套解析逻辑：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": { },
  "timestamp": "2026-09-13T17:47:11.225090"
}
```

```yaml
# ==============================================
# 响应字段契约说明
# ==============================================
fields:
  code: 0                        # 业务码，0 表示成功，非 0 为错误码
  message: 操作成功               # 人类可读提示，可直接展示给用户
  data: {}                       # 业务数据，类型随接口而变
  timestamp: 2026-09-13T17:47:11 # 服务端时间，ISO-8601 格式

# ==============================================
# 序列化约定
# ==============================================
serialization:
  date-format: ISO-8601          # write-dates-as-timestamps: false
  time-zone: GMT+8               # java.time.* 类型的时区基准
  null-handling: non_null        # 全局：值为 null 的字段不出现在 JSON 中
  impact: 前端类型中这些字段统一声明为可选（?:）

# ==============================================
# 重要例外：ApiResponse 类级别的 @JsonInclude(ALWAYS)
# ==============================================
exception-rule:
  annotation: '@JsonInclude(JsonInclude.Include.ALWAYS)'
  scope: 仅作用于 ApiResponse 自身的 4 个字段
  effect: data 即使为 null 也会显式输出 "data": null
  reason: 保证响应结构恒定，前端解包逻辑无需判断 data 键是否存在
  contrast: |
    内层业务对象仍遵循全局 non_null。例如：
    - FieldValidationError.rejectedValue 为 null 时不出现在明细中
    - TodoResponse.completedAt 为 null（未完成）时同样被省略
```

分页响应的 `data` 结构（`PageResult<T>`，共 8 个字段）：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "content": [ ],
    "page": 1,
    "size": 10,
    "totalElements": 13,
    "totalPages": 2,
    "first": true,
    "last": false,
    "empty": false
  },
  "timestamp": "2026-09-13T17:47:11.225090"
}
```

```yaml
# ==============================================
# PageResult 字段说明
# ==============================================
page-result-fields:
  content: []                    # 当前页数据列表
  page: 1                        # 当前页码，从 1 开始（已由 0-based 转换）
  size: 10                       # 每页条数
  totalElements: 13              # 总记录数
  totalPages: 2                  # 总页数
  first: true                    # 是否为首页
  last: false                    # 是否为末页
  empty: false                   # 当前页是否为空
```

### 4.4 错误码清单

> [⬆ 返回本章导航](#四接口文档) | [⬆ 返回目录导航](#目录导航)

错误码为 5 位数字，**前 3 位与 HTTP 状态码对齐**，便于快速定位问题层级：

```text
# ==============================================
# 错误码编码规则
# ==============================================
  4 0 0 0 1
  └───┬──┘│
      │   └── 序号：同一 HTTP 状态下的第几种业务错误
      └────── HTTP 状态码：400 / 404 / 405 / 409 / 500
```

| 错误码 | 枚举常量 | HTTP | 默认提示文案 | `data` 内容 |
| --- | --- | --- | --- | --- |
| `0` | `SUCCESS` | 200 | 操作成功 | 业务数据 |
| `40000` | `BAD_REQUEST` | 400 | 请求参数错误 | `null` |
| `40001` | `VALIDATION_FAILED` | 400 | 参数校验失败 | **字段级错误明细数组** |
| `40002` | `MESSAGE_NOT_READABLE` | 400 | 请求体格式错误，无法解析 | `null` |
| `40003` | `TYPE_MISMATCH` | 400 | 参数类型不匹配 | `null` |
| `40400` | `NOT_FOUND` | 404 | 请求的资源不存在 | `null` |
| `40401` | `TODO_NOT_FOUND` | 404 | 待办事项不存在 | `null` |
| `40402` | `USER_NOT_FOUND` | 404 | 用户不存在 | `null` |
| `40500` | `METHOD_NOT_ALLOWED` | 405 | 不支持的请求方法 | `null` |
| `40900` | `CONFLICT` | 409 | 资源状态冲突 | `null` |
| `40901` | `USERNAME_DUPLICATED` | 409 | 用户名已存在 | `null` |
| `40902` | `EMAIL_DUPLICATED` | 409 | 邮箱已被占用 | `null` |
| `40903` | `RESOURCE_IN_USE` | 409 | 该资源存在关联数据，无法删除 | `null` |
| `50000` | `INTERNAL_ERROR` | 500 | 服务器内部错误，请稍后重试 | `null`（不泄漏内部细节） |
| `50300` | `SERVICE_UNAVAILABLE` | 503 | 依赖服务暂不可用 | `null` |

```text
# ==============================================
# 易混淆码位辨析
# ==============================================
40000 BAD_REQUEST          通用参数错误（缺少必填请求参数、方法级约束违反）
40001 VALIDATION_FAILED    唯一会返回字段级明细数组的码
40002 MESSAGE_NOT_READABLE 请求体本身无法解析（JSON 语法错误）
40003 TYPE_MISMATCH        能解析但类型不匹配（枚举值非法、字符串传给数字）

40400 NOT_FOUND            通用资源不存在（含静态资源与未匹配路径）
40401 / 40402              待办 / 用户的专用不存在码，提示更精确

40900 CONFLICT             通用冲突（数据库完整性约束被违反）
40901 / 40902 / 40903      用户名重复 / 邮箱重复 / 被关联数据引用

# ==============================================
# 重要：为什么 40901、40902 的 data 是 null
# ==============================================
BusinessException 只携带 errorCode 与 message，不携带 data
因此 40901、40902 虽在语义上对应具体字段，
响应体中 data 仍为 null（受 @JsonInclude(ALWAYS) 影响显式输出 "data": null）

前端影响：唯一性冲突无法依赖 details 定位字段，
          需按 code 自行映射（40901 → username，40902 → email）
```

参数校验失败时（**仅 `40001`**），`data` 会返回字段级明细，前端据此把错误直接标注到对应表单项上：

```json
{
  "code": 40001,
  "message": "标题不能为空",
  "data": [
    { "field": "title", "message": "标题不能为空", "rejectedValue": "" }
  ]
}
```

```yaml
# ==============================================
# 字段级错误明细结构（FieldValidationError）
# ==============================================
detail-fields:
  field: title                   # 出错的字段名，与前端表单 prop 一致
  message: 标题不能为空           # 校验注解上声明的提示文案
  rejectedValue: ''              # 被拒绝的原始值，便于排查
```

## 五、后端设计说明

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[5.1 全局异常处理机制](#51-全局异常处理机制) | [5.2 分页与排序约定](#52-分页与排序约定) | [5.3 分层职责划分](#53-分层职责划分) | [5.4 N+1 查询规避](#54-n1-查询规避) | [5.5 演示数据初始化](#55-演示数据初始化) | [5.6 多环境配置](#56-多环境配置)

### 5.1 全局异常处理机制

> [⬆ 返回本章导航](#五后端设计说明) | [⬆ 返回目录导航](#目录导航)

`GlobalExceptionHandler`（`@RestControllerAdvice`）集中兜底，Controller 内**不需要写任何 try-catch**：

```text
# ==============================================
# 异常类型 → 错误码映射表（共 12 个 @ExceptionHandler）
# ==============================================
BusinessException                        →  按异常携带的 ErrorCode 返回
EntityNotFoundException                  →  40400 NOT_FOUND（JPA 实体未找到）
MethodArgumentNotValidException          →  40001 VALIDATION_FAILED + 字段明细
                                           （@RequestBody 上的 @Valid 校验失败）
BindException                            →  40001 VALIDATION_FAILED + 字段明细
                                           （@ModelAttribute 表单绑定校验失败）
ConstraintViolationException             →  40000 BAD_REQUEST（方法级参数约束违反）
MissingServletRequestParameterException  →  40000 BAD_REQUEST（缺少必填请求参数）
MethodArgumentTypeMismatchException      →  40003 TYPE_MISMATCH（例如枚举值非法）
HttpMessageNotReadableException          →  40002 MESSAGE_NOT_READABLE（JSON 解析失败）
HttpRequestMethodNotSupportedException   →  40500 METHOD_NOT_ALLOWED（请求方法不支持）
NoResourceFoundException                 →  40400 NOT_FOUND（静态资源或路径未命中）
DataIntegrityViolationException          →  40900 CONFLICT（唯一约束冲突、字段超长等）
Exception                                →  50000 INTERNAL_ERROR（兜底）
```

```yaml
# ==============================================
# 兜底异常处理原则
# ==============================================
principles:
  logging: 记录完整堆栈到服务端日志        # 便于排查
  response: 仅返回通用提示文案             # 不泄漏内部实现细节
  reason: 堆栈、SQL、类名等信息对外暴露存在安全风险

# ==============================================
# 提示文案的定制方式
# ==============================================
message-override:
  mechanism: build(ErrorCode, 自定义文案, data)
  examples:
    HttpMessageNotReadableException: 请求体格式错误，请检查 JSON 是否合法
    NoResourceFoundException: 请求的接口或资源不存在：{requestURI}
    DataIntegrityViolationException: 数据违反完整性约束，请检查提交内容是否重复或超长
  benefit: 复用错误码的 HTTP 状态与语义，同时给出更贴合场景的用户提示
```

### 5.2 分页与排序约定

> [⬆ 返回本章导航](#五后端设计说明) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 页码转换链路
# ==============================================
前端 el-pagination (1-based)
        │
        ▼  query.page = 1
PageQuery.toPageable()
        │
        ▼  PageRequest.of(0, size, sort)   ← 减 1 转为 Spring Data 的 0-based
Spring Data JPA
        │
        ▼  Page<T>（内部 0-based）
PageResult.from(page)
        │
        ▼  page = 1                        ← 加 1 回写为 1-based
前端直接使用，无需换算
```

对外 API 的 `page` 从 **1** 开始（与 Element Plus 分页组件语义一致），内部由 `PageQuery.toPageable()` 转换为 Spring Data 的 0-based 页码；响应的 `PageResult.page` 同样回写为 1-based。前后端无需各自记住一套偏移规则。

`size` 上限为 100，超出会被截断，防止一次请求拖垮数据库。

```java
// ==============================================
// 排序字段白名单机制
// ==============================================
// 问题：sortBy 直接来自前端，若原样拼进 Sort.by()，
//       遇到非法字段会抛出 PropertyReferenceException（500）
// 方案：用各查询 DTO 声明的白名单做校验，
//       未命中时静默回退到默认排序字段
// 收益：既不报错，也不会被注入非法属性名

// TodoQueryRequest 中声明的白名单
public static final String[] SORTABLE_FIELDS = {
    "id", "title", "status", "priority", "dueDate", "completedAt", "createdAt", "updatedAt"
};
```

### 5.3 分层职责划分

> [⬆ 返回本章导航](#五后端设计说明) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 分层职责与依赖方向
# ==============================================
Controller  →  只做参数绑定、校验触发、响应包装，不写业务逻辑
Service     →  业务规则、事务边界、跨 Repository 编排
Repository  →  数据访问，复杂条件交给 Specification
Mapper      →  Entity ↔ DTO 转换（手写，无 MapStruct 依赖，便于调试）
DTO         →  Record（请求/响应）+ Lombok 类（查询条件，需可变以支持 @ModelAttribute 绑定）

依赖方向：Controller → Service → Repository，禁止反向依赖与跨层调用
```

```yaml
# ==============================================
# 关键设计约定
# ==============================================
conventions:
  entity-exposure: 禁止             # 实体不直接对外暴露
  reason:                          # 原因说明
    - 避免懒加载代理序列化异常       # LazyInitializationException
    - 避免内部字段（如密码）泄漏     # 安全性
  dto-type: Java Record            # 请求/响应 DTO 使用不可变 Record
  query-dto-type: Lombok 类         # 查询条件需可变以支持表单绑定
  table-naming:                    # 表名约定
    user: sys_user                 #   规避 user 保留字
    todo: todo_item                #   语义清晰
  concurrency-control: '@Version'  # 实体启用乐观锁
  audit: JPA Auditing              # createdAt / updatedAt 自动填充
```

### 5.4 N+1 查询规避

> [⬆ 返回本章导航](#五后端设计说明) | [⬆ 返回目录导航](#目录导航)

项目中有两处典型的 N+1 场景，均已针对性处理：

```text
# ==============================================
# 场景一：待办列表的负责人信息
# ==============================================
问题：每行待办访问 assignee 时触发一次额外 SELECT
      10 行数据 = 1 次主查询 + 10 次关联查询
方案：TodoSpecifications 中按需追加 LEFT JOIN FETCH
注意点：count 查询必须跳过 fetch
        （count 查询带 fetch 会被 Hibernate 忽略并打印告警）

# ==============================================
# 场景二：用户列表的待办数量
# ==============================================
问题：每行用户执行一次 COUNT 查询
      10 行数据 = 1 次主查询 + 10 次聚合查询
方案：UserServiceImpl.loadTodoCounts()
      用一次 GROUP BY assignee_id 聚合查询
      拿到当前页所有用户的计数，再在内存中填充
效果：无论每页多少行，待办计数始终只有 1 次查询
```

```java
// ==============================================
// 场景二真实实现：TodoRepository.countGroupByAssignee
// ==============================================
// 使用接口投影 AssigneeTodoCount 接收聚合结果，避免为统计单独建 DTO 类
@Query("""
        select t.assignee.id as assigneeId, count(t) as total
        from TodoItem t
        where t.assignee.id in :assigneeIds
        group by t.assignee.id
        """)
List<AssigneeTodoCount> countGroupByAssignee(@Param("assigneeIds") Collection<Long> assigneeIds);
```

```java
// ==============================================
// UserServiceImpl.loadTodoCounts：聚合结果转 Map
// ==============================================
private Map<Long, Long> loadTodoCounts(List<User> users) {
    // 1. 提取当前页所有用户 ID，作为一次聚合查询的入参
    List<Long> userIds = users.stream().map(User::getId).toList();

    // 2. 单次 GROUP BY 查询取回全部计数，收集为 Map
    //    key: assigneeId（负责人 ID），value: 该负责人名下的待办数量
    //    第三个参数为合并函数：理论上 GROUP BY 不会产生重复 key，
    //    保留已有值仅为满足 Collectors.toMap 的签名要求
    return todoRepository.countGroupByAssignee(userIds).stream()
            .collect(Collectors.toMap(
                    AssigneeTodoCount::getAssigneeId,
                    AssigneeTodoCount::getTotal,
                    (existing, replacement) -> existing,
                    HashMap::new
            ));
}

// 3. 调用方按 ID 取值，无待办的用户回退为 0
//    （GROUP BY 结果中不会包含计数为 0 的用户，因此必须用 getOrDefault）
UserMapper.toResponse(user, todoCountMap.getOrDefault(user.getId(), 0L))
```

### 5.5 演示数据初始化

> [⬆ 返回本章导航](#五后端设计说明) | [⬆ 返回目录导航](#目录导航)

`DataInitializer` 是一个 `ApplicationRunner`，受 `app.seed-data` 开关控制（默认开启）：

```yaml
# ==============================================
# 演示数据与 CORS 配置（application.yml 真实内容）
# ==============================================
app:
  cors:
    allowed-origins:                     # 允许的来源，列表形式
      - http://localhost:5173
      - http://127.0.0.1:5173
    allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
    allowed-headers: "*"
    allow-credentials: true
    max-age: 3600                        # 预检结果缓存时间（秒）
  seed-data: true                        # 是否灌入演示数据
                                         # prod / test profile 中覆盖为 false
                                         # 亦可用环境变量 APP_SEED_DATA=false 覆盖
```

```text
# ==============================================
# 演示数据内容与覆盖场景
# ==============================================
用户数据：6 条
  ├─ 覆盖全部 4 种角色（ADMIN / MANAGER / MEMBER / GUEST）
  ├─ 覆盖 5 个部门（技术管理部 / 研发部 / 设计部 / 测试部 / 运营部）
  ├─ 覆盖 ACTIVE 与 DISABLED 两种状态
  └─ 其中 1 个为 DISABLED，用于验证下拉选项过滤

待办数据：13 条
  ├─ 已逾期            用于验证 overdue 筛选与仪表盘逾期卡片
  ├─ 今日到期          用于验证 dueTodayCount 统计
  ├─ 进行中            用于验证状态流转路径
  ├─ 待处理            用于验证默认筛选视图
  ├─ 已完成            用于验证 completedAt 自动写入与完成率计算
  ├─ 已取消            用于验证终态统计与恢复流转
  ├─ 未分配负责人       用于验证 unassigned 筛选
  └─ 标题「编写项目周报」 与 Swagger 中 keyword 参数示例值一致
                        可直接用于验证关键字搜索
```

演示用户明细（可用于接口调试与前端联调）：

| id | username | 昵称 | 部门 | 角色 | 状态 | 待办数 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `admin` | 系统管理员 | 技术管理部 | `ADMIN` | `ACTIVE` | 2 | 拥有全部权限的内置管理员账号 |
| 2 | `zhangsan` | 张三 | 研发部 | `MANAGER` | `ACTIVE` | 4 | 负责后端迭代排期 |
| 3 | `lisi` | 李四 | 研发部 | `MEMBER` | `ACTIVE` | 4 | 前端开发 |
| 4 | `wangwu` | 王五 | 设计部 | `MEMBER` | `ACTIVE` | 2 | UI / 交互设计 |
| 5 | `zhaoliu` | 赵六 | 测试部 | `MEMBER` | `DISABLED` | 0 | 已离职，账号停用 |
| 6 | `guest` | 访客账号 | 运营部 | `GUEST` | `ACTIVE` | 0 | 只读演示账号 |

> 待办数合计 12，加上 1 条未分配负责人的待办 = 13，与总数自洽。
> `zhaoliu` 为 `DISABLED`，因此 `GET /api/v1/users/options` 只返回其余 5 个用户——前端「负责人」下拉正是 5 项。

> 生产环境请通过 `APP_SEED_DATA=false` 关闭演示数据灌入。

### 5.6 多环境配置

> [⬆ 返回本章导航](#五后端设计说明) | [⬆ 返回目录导航](#目录导航)

| Profile | 用途 | 数据源 | `ddl-auto` | SQL 日志 | 演示数据 | H2 控制台 | 接口文档 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `dev`（默认） | 本地开发 | H2 **内存** `mem:taskhub` | `create-drop` | 打印（含绑定参数） | 开启 | 开启 | 开启 |
| `prod` | 生产 | H2 **文件** `file:./data/taskhub` | `update` | 关闭 | 关闭 | 关闭 | **已关闭** |
| `test` | 测试 | H2 **内存** `mem:taskhub-test` | `create-drop` | 关闭 | 关闭 | 关闭 | 关闭 |

```yaml
# ==============================================
# 各 Profile 的完整差异（取自真实配置文件）
# ==============================================
dev:                               # application-dev.yml
  datasource-url: jdbc:h2:mem:taskhub;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL
  hikari: maximum-pool-size 10 / minimum-idle 2      # 开发环境连接池较小
  h2-console: enabled true, path /h2-console
  devtools-restart: false                            # 关闭自动重启，避免与手动调试冲突
  logging:
    root: INFO
    com.example.taskhub: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: DEBUG               # 打印 SQL 绑定参数，便于排查

prod:                              # application-prod.yml
  datasource-url: '${APP_DB_URL:jdbc:h2:file:./data/taskhub;AUTO_SERVER=TRUE;MODE=MySQL}'
  hikari: maximum-pool-size 20 / minimum-idle 5      # 生产环境连接池更大
  cors-allowed-origins: '${APP_CORS_ALLOWED_ORIGINS:https://taskhub.example.com}'
  springdoc: api-docs 与 swagger-ui 均已在配置中写死 false
  management:
    exposure: health,info                            # 收窄端点暴露范围
    health-show-details: never                       # 不对外暴露健康详情
  logging:
    root: WARN
    com.example.taskhub: INFO
    file: ./logs/taskhub-backend.log                 # 输出到文件
    rollingpolicy: max-file-size 50MB / max-history 30   # 日志滚动策略

test:                              # src/test/resources/application-test.yml
  datasource-url: jdbc:h2:mem:taskhub-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL
  cors-allowed-origins: http://localhost:5173
  management-exposure: health                        # 仅保留健康检查

common:                            # application.yml（所有 Profile 共享）
  server-port: 8080
  server-shutdown: graceful                          # 优雅停机，等待在途请求完成
  tomcat-threads-max: 200
  jpa-open-in-view: false                            # 关闭 OSIV，避免视图层触发懒加载占用连接
  app-seed-data: true                                # 默认开启，被 prod / test 覆盖为 false
  management-exposure: health,info,metrics
  springdoc-swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method                        # 按 HTTP 方法排序
    tags-sorter: alpha
    display-request-duration: true                   # 显示请求耗时
    doc-expansion: list
```

```shell
# ------------------------------------------
# Profile 切换方式一：命令行参数
# ------------------------------------------
java -jar app.jar --spring.profiles.active=prod

# ------------------------------------------
# Profile 切换方式二：环境变量（容器部署推荐）
# ------------------------------------------
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

```yaml
# ==============================================
# 生产环境建议覆盖的配置项
# ==============================================
SPRING_PROFILES_ACTIVE: prod                       # 激活生产 profile
SERVER_PORT: 8080                                  # 服务端口
APP_SEED_DATA: 'false'                             # 关闭演示数据（prod 已默认 false，此为双保险）
APP_CORS_ALLOWED_ORIGINS: https://your.domain      # 收紧 CORS 白名单为真实前端域名
APP_DB_URL: jdbc:mysql://host:3306/taskhub         # 接入真实数据库时替换（prod 默认 H2 文件）
APP_DB_USERNAME: taskhub                           # 数据库用户名
APP_DB_PASSWORD: ******                            # 数据库密码
```

```text
# ==============================================
# 关于环境变量覆盖机制（已实测验证）
# ==============================================
现象：application.yml 中 server.port 与 app.seed-data 均为硬编码值，
      并未写成 ${SERVER_PORT:8080} 这类占位符形式

结论：仍可通过环境变量覆盖，无需修改 yml

原理：Spring Boot 的宽松绑定（Relaxed Binding）
      环境变量 SERVER_PORT   → 映射到 server.port
      环境变量 APP_SEED_DATA → 映射到 app.seed-data

实测验证：
  APP_SEED_DATA=false SERVER_PORT=8099 java -jar app.jar --spring.profiles.active=dev
  ├─ 日志输出 Tomcat initialized with port 8099     → 端口覆盖生效
  └─ GET /api/v1/users 返回 totalElements = 0       → 演示数据未灌入，覆盖生效

仅以下 4 项在 yml 中显式使用了占位符（均在 application-prod.yml）：
  ${APP_DB_URL:...}  ${APP_DB_USERNAME:sa}  ${APP_DB_PASSWORD:}  ${APP_CORS_ALLOWED_ORIGINS:...}
```


## 六、前端设计说明

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[6.1 Axios 请求封装](#61-axios-请求封装) | [6.2 表格逻辑复用](#62-表格逻辑复用) | [6.3 枚举字典管理](#63-枚举字典管理) | [6.4 日期时间格式化](#64-日期时间格式化) | [6.5 查询参数清洗](#65-查询参数清洗) | [6.6 响应式布局断点](#66-响应式布局断点) | [6.7 路由与菜单单一数据源](#67-路由与菜单单一数据源)

### 6.1 Axios 请求封装

> [⬆ 返回本章导航](#六前端设计说明) | [⬆ 返回目录导航](#目录导航)

`src/api/request.ts` 承担了四件事：

```text
# ==============================================
# Axios 封装职责
# ==============================================
1. 统一实例
   ├─ baseURL 取自 VITE_API_BASE_URL（默认 /api/v1）
   ├─ 超时 15 秒
   └─ 默认 Content-Type: application/json

2. 响应解包
   ├─ 拦截器校验 code !== 0 时抛出 ApiError
   └─ 成功时把 body.data 提升到 response.data
      业务代码拿到的直接是数据本身，无需层层 .data

3. 统一错误提示
   ├─ ElMessage.error 自动弹出
   └─ 对 1 秒内的重复提示做抑制
      避免批量请求同时失败时刷屏

4. 可按需静默
   ├─ 扩展了 AxiosRequestConfig.silent 字段
   └─ 表单提交类请求设为 silent: true
      把服务端的字段级错误回填到具体表单项上
      而不是弹一个笼统的 Toast
```

上层通过类型安全的 `http` 对象调用，泛型即业务数据类型，无需手动断言：

```ts
// ------------------------------------------
// 调用示例：泛型直接标注业务数据类型
// ------------------------------------------
const page = await http.get<PageResult<TodoItem>>('/todos', { params })

// ------------------------------------------
// 静默模式示例：自行处理字段级错误
// ------------------------------------------
try {
  await http.post<TodoResponse>('/todos', payload, { silent: true })
} catch (e) {
  const error = e as ApiError
  // 把 40001 的字段明细回填到对应表单项
  error.details.forEach((d) => (serverErrors[d.field] = d.message))
}
```

```yaml
# ==============================================
# ApiError 类结构（src/types/api.ts）
# ==============================================
ApiError:
  extends: Error                 # 继承原生 Error
  properties:
    code: 40001                  # 业务错误码
    httpStatus: 400              # HTTP 状态码
    details: []                  # 字段级错误明细数组
  getters:
    isValidationError: true      # code === 40001 时为 true
  methods:
    fieldMessage: 传入字段名，返回该字段的错误提示  # 便捷方法
```

### 6.2 表格逻辑复用

> [⬆ 返回本章导航](#六前端设计说明) | [⬆ 返回目录导航](#目录导航)

`src/composables/useTable.ts` 把每个列表页都会重复的样板逻辑收敛到一处：

```ts
// ------------------------------------------
// 使用方式：解构即可获得完整表格能力
// ------------------------------------------
const {
  loading, list, total, query,
  search, reset,
  handleCurrentChange, handleSizeChange, handleSortChange,
} = useTable({
  fetcher: pageTodos,        // 分页请求函数
  defaultQuery: DEFAULT_QUERY, // 默认查询条件（reset 时回到此状态）
  immediate: true,           // 是否在 onMounted 自动加载（默认 true）
})
```

除常规的加载态与分页外，还处理了两个容易踩的坑：

```text
# ==============================================
# 坑一：请求竞态
# ==============================================
现象：快速连续触发查询时，先发出的慢请求可能后返回
      导致旧数据覆盖新数据
方案：内部维护自增请求序号 requestSeq
      响应回来时比对序号，只采用最后一次请求的结果
      过期响应直接丢弃，loading 状态也仅由最新请求控制

# ==============================================
# 坑二：排序值映射
# ==============================================
现象：el-table 的排序事件给的是 ascending / descending
      后端期望的是 asc / desc，两者不一致
方案：handleSortChange 内部做映射转换
      取消排序时清空 sortBy，交回后端默认排序字段
```

```yaml
# ==============================================
# useTable 配置项说明
# ==============================================
options:
  fetcher: 分页请求函数，接收 query 返回 PageResult   # 必填
  defaultQuery: 默认查询条件对象                      # 必填
  immediate: 是否在 onMounted 自动加载，默认 true      # 可选
  onError: 请求失败回调                               # 可选
note: |
  若页面需要在首次加载后基于结果做额外处理
  （例如 UserListView 收集部门候选项），
  应传 immediate: false，改为在自己的 onMounted 中
  await loadData() 后再处理，避免并发两次请求
```

### 6.3 枚举字典管理

> [⬆ 返回本章导航](#六前端设计说明) | [⬆ 返回目录导航](#目录导航)

后端是 Java 枚举，前端在 `src/utils/dict.ts` 维护一份对应的中文标签与 `el-tag` 颜色映射：

```yaml
# ==============================================
# 字典项结构（DictOption<T>）
# ==============================================
DictOption:
  value: PENDING                 # 枚举值，与后端严格一致
  label: 待处理                   # 中文展示文案
  tagType: info                  # el-tag 的 type，决定颜色
  hint: 尚未开始                  # 可选，鼠标悬浮提示

# ==============================================
# 已定义的字典（与后端枚举一一对应）
# ==============================================
dicts:
  TODO_STATUS_OPTIONS:           # 对应后端 TodoStatus
    PENDING:     待处理   (tagType: info)      # hint: 尚未开始
    IN_PROGRESS: 进行中   (tagType: primary)   # hint: 正在处理
    DONE:        已完成   (tagType: success)   # hint: 已交付，服务端会写入完成时间
    CANCELLED:   已取消   (tagType: danger)    # hint: 不再执行

  TODO_PRIORITY_OPTIONS:         # 对应后端 TodoPriority（含 weight 权重）
    LOW:    低    (tagType: info,     weight: 1)
    MEDIUM: 中    (tagType: primary,  weight: 2)
    HIGH:   高    (tagType: warning,  weight: 3)
    URGENT: 紧急  (tagType: danger,   weight: 4)

  USER_ROLE_OPTIONS:             # 对应后端 UserRole
    ADMIN:   管理员  (tagType: danger)    # hint: 系统最高权限
    MANAGER: 经理    (tagType: warning)   # hint: 团队管理权限
    MEMBER:  成员    (tagType: primary)   # hint: 普通业务人员
    GUEST:   访客    (tagType: info)      # hint: 只读权限

  USER_STATUS_OPTIONS:           # 对应后端 UserStatus
    ACTIVE:   启用  (tagType: success)   # hint: 账号可正常使用
    DISABLED: 禁用  (tagType: info)      # hint: 账号已停用，不可被指派待办
    LOCKED:   锁定  (tagType: danger)    # hint: 因安全策略被临时锁定

  PAGE_SIZE_OPTIONS: '[10, 20, 50, 100]'   # 每页条数候选
```

所有下拉选项、标签渲染都从这里取值，避免文案散落在各个组件中不一致。
其中 `nextStatusOptions()` 还定义了**状态流转路径**，行内操作只会展示有效的下一步：

```text
# ==============================================
# 状态流转路径定义（nextStatusOptions）
# ==============================================
PENDING     →  IN_PROGRESS / DONE / CANCELLED   # 可开始处理、直接完成或取消
IN_PROGRESS →  DONE / PENDING / CANCELLED       # 可完成、退回待处理或取消
DONE        →  IN_PROGRESS                      # 已完成可【重新打开】
CANCELLED   →  PENDING                          # 已取消可【恢复】为待处理

# ==============================================
# 注意：DONE 与 CANCELLED 并非流转终态
# ==============================================
TodoStatus 枚举带有 finished 布尔标记：
  PENDING(false) / IN_PROGRESS(false) / DONE(true) / CANCELLED(true)

该标记用于【统计口径】而非【流转限制】，但需注意两处口径并不相同：

1. 逾期与今日到期统计 —— 使用 UNFINISHED_STATUSES
   ├─ countByDueDateBeforeAndStatusIn(today, UNFINISHED)  → 逾期数
   ├─ countByDueDateAndStatusIn(today, UNFINISHED)        → 今日到期数
   └─ 含义：已完成与已取消的待办即使日期已过，也不再计入逾期

2. 完成率计算 —— 分子【仅 DONE】，不含 CANCELLED
   ├─ completionRate(total, statusCounts.get(DONE))
   ├─ 公式：Math.round(doneCount * 10000.0 / total) / 100.0（保留两位小数）
   ├─ total 为 0 时返回 0.0，规避除零
   └─ 含义：已取消属于「未交付」，不应被算作完成

3. UI 流转仍允许 DONE 重开、CANCELLED 恢复
   └─ 以支持「误操作完成」「取消后需求复活」等真实场景
```

### 6.4 日期时间格式化

> [⬆ 返回本章导航](#六前端设计说明) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 问题背景
# ==============================================
后端输出的 LocalDateTime 形如 2026-09-13T10:30:00（不带时区后缀）
        │
        ▼  交给 new Date() 解析
不同浏览器对「无时区字符串」的处理并不一致
  ├─ 部分按本地时间解析
  └─ 部分按 UTC 解析
        │
        ▼  结果
出现「显示时间比录入时间差 8 小时」的经典问题

# ==============================================
# 解决方案（src/utils/format.ts）
# ==============================================
采用纯正则字符串解析，直接提取年月日时分秒各段
不做任何时区换算，所见即所得
```

```ts
// ------------------------------------------
// 解析用正则：分别捕获年月日时分秒
// ------------------------------------------
const ISO_PATTERN = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/
```

| 导出函数 | 用途 |
| --- | --- |
| `formatDateTime` | 完整日期时间 `2026-09-13 10:30:00` |
| `formatDateTimeShort` | 精简日期时间 `2026-09-13 10:30` |
| `formatDate` | 仅日期 `2026-09-13` |
| `today` | 获取今日日期字符串 |
| `diffDays` | 计算与今日相差天数 |
| `describeDueDate` | 生成「已逾期 3 天」「今日到期」等语义化描述 |

### 6.5 查询参数清洗

> [⬆ 返回本章导航](#六前端设计说明) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 问题背景
# ==============================================
Element Plus 控件清空后会把值置为 '' 或 null
        │
        ▼  直接发送请求
后端收到 status= 这类脏参数
        │
        ▼  枚举绑定时
抛出 400 错误

# ==============================================
# 解决方案（src/utils/params.ts）
# ==============================================
cleanParams() 在发请求前统一剔除空值
```

```yaml
# ==============================================
# cleanParams 剔除规则
# ==============================================
removed:                         # 会被剔除的值
  - undefined                    #   未定义
  - null                         #   空值
  - ''                           #   空字符串
  - '[]'                         #   空数组

kept:                            # 会被保留的值（关键）
  - 'false'                      #   布尔假值，如 overdue=false 是有效筛选
  - '0'                          #   数字零，如 page=0 或统计值 0

signature: 'cleanParams<T extends object>(params: T): T'
note: 泛型约束为 object 而非索引签名类型，
      以便直接传入 UserQuery / TodoQuery 等具名接口而不报类型错误
```

### 6.6 响应式布局断点

> [⬆ 返回本章导航](#六前端设计说明) | [⬆ 返回目录导航](#目录导航)

| 断点 | 行为 |
| --- | --- |
| `> 1200px` | 侧边栏常驻，统计卡片 4 列 |
| `≤ 1200px` | 内边距收紧，统计卡片降为 2 列 |
| `≤ 992px` | 顶栏隐藏页面说明文字 |
| `≤ 768px` | 侧边栏改为**抽屉浮层**，筛选表单单列，分页器隐藏 sizes/jumper |

```yaml
# ==============================================
# 响应式实现方式
# ==============================================
breakpoint-detection:
  composable: useMediaQuery()    # 基于 window.matchMedia
  advantage: 由浏览器原生事件驱动，比监听 resize 手动比较宽度更省开销

sidebar-state:
  store: Pinia                   # appStore
  persistence: localStorage      # 桌面端折叠状态跨会话保留

mobile-drawer:
  trigger: isMobile 为 true      # 视口 ≤ 768px
  behavior: 侧边栏渲染为 el-drawer（ltr 方向）
  auto-close: watch(isMobile)，切回桌面端时自动关闭抽屉

layout-safeguard:
  rule: '&__body { min-width: 0 }'
  reason: flex 子项默认 min-width 为 auto，
          内部宽表格会撑破容器导致横向溢出
```

设计令牌（颜色、间距、圆角、阴影、过渡时长）以 **CSS 自定义属性**形式定义在 `:root`，而不是 Sass 变量 + `additionalData` 注入——前者可被运行时主题覆盖，也不会让每个组件都隐式依赖一份全局变量表。

### 6.7 路由与菜单单一数据源

> [⬆ 返回本章导航](#六前端设计说明) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 单一数据源派生链路
# ==============================================
router/index.ts 导出 mainRoutes
        │
        ├──→ SideMenu.vue 遍历生成菜单项（图标、标题、路径）
        ├──→ DefaultLayout.vue 面包屑
        └──→ afterEach 钩子设置 document.title

收益：新增页面时只改路由表一处，菜单、面包屑、页面标题自动跟上
```

```yaml
# ==============================================
# 路由设计要点
# ==============================================
route-meta:
  declaration: 通过 declare module 'vue-router' 做接口声明合并
  fields: title / icon / description / hidden

lazy-loading: 所有页面组件使用 () => import() 动态导入   # 按路由分包

history-fallback:
  mode: createWebHistory
  note: 生产环境 Nginx 必须配置 try_files 回退到 index.html

not-found:
  path: '/:pathMatch(.*)*'
  component: NotFoundView
  placement: 作为顶层路由，不在 DefaultLayout 内
  reason: 404 页面不需要侧边栏与顶栏

scroll-behavior: 路由切换后回到页面顶部
```

## 七、测试说明

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[7.1 后端测试](#71-后端测试) | [7.2 前端类型检查与构建](#72-前端类型检查与构建)

### 7.1 后端测试

> [⬆ 返回本章导航](#七测试说明) | [⬆ 返回目录导航](#目录导航)

```shell
# ------------------------------------------
# 执行全部后端测试
# ------------------------------------------
cd backend
mvn test
```

当前共 **58 个用例，全部通过**：

| 测试类 | 类型 | 用例数 | 覆盖内容 |
| --- | --- | --- | --- |
| `TodoApiIntegrationTest` | MockMvc 集成 | 27 | 分页/筛选/排序、CRUD、状态流转、批量删除、聚合统计、参数校验、404、枚举非法值 |
| `UserApiIntegrationTest` | MockMvc 集成 | 18 | 分页/筛选、CRUD、状态变更、唯一性冲突（40901/40902）、关联占用（40903） |
| `TodoServiceImplTest` | Mockito 单元 | 11 | 业务分支与边界：完成率计算、批量删除的缺失 ID 校验、逾期判定 |
| `TaskHubApplicationTests` | 上下文 | 2 | Spring 上下文加载、关键 Bean 装配 |

```yaml
# ==============================================
# 测试隔离策略（application-test.yml）
# ==============================================
isolation:
  profile: '@ActiveProfiles("test")'   # 激活测试专用配置
  transaction: '@Transactional'        # 每个用例执行后自动回滚
  ddl-auto: create-drop                # 每次重建表结构
  seed-data: false                     # 关闭演示数据，用例自行构造数据
  springdoc: 关闭                       # 测试环境无需生成接口文档
benefit: 用例之间互不干扰，可任意顺序执行，也可单独执行某一个
```

### 7.2 前端类型检查与构建

> [⬆ 返回本章导航](#七测试说明) | [⬆ 返回目录导航](#目录导航)

```shell
# ------------------------------------------
# 类型检查（不产出文件，仅校验）
# ------------------------------------------
cd frontend
npm run type-check       # vue-tsc --noEmit，全量 strict 模式检查

# ------------------------------------------
# 生产构建（内含类型检查）
# ------------------------------------------
npm run build            # 类型检查 + Vite 打包，产物输出到 dist/
```

```yaml
# ==============================================
# TypeScript 严格配置（tsconfig.json）
# ==============================================
compiler-options:
  strict: true                       # 开启全部严格类型检查选项
  noUnusedLocals: true               # 禁止未使用的局部变量
  noUnusedParameters: true           # 禁止未使用的函数参数
  verbatimModuleSyntax: true         # 强制区分 type import 与 value import
  paths:
    '@/*': ['./src/*']               # 路径别名

# ==============================================
# 构建产物实测数据（vite build 真实输出）
# ==============================================
build-result:
  modules-transformed: 1699          # 转换模块数
  build-time: 约 350ms               # 构建耗时
  dist-size: 1.6 MB                  # 产物目录总体积
```

产物分包明细（`原体积 │ gzip 后`）：

```text
# ==============================================
# 第三方依赖 chunk（长缓存，文件名带 hash）
# ==============================================
element-*.js      1,131.19 kB │ gzip: 353.55 kB   # Element Plus 全量引入
element-*.css       359.98 kB │ gzip:  47.92 kB   # Element Plus 样式
vue-*.js             30.72 kB │ gzip:  12.05 kB   # Vue + Router + Pinia

# ==============================================
# 共享业务 chunk（被多个视图依赖，由 Vite 自动提取）
# ==============================================
format-*.js          54.95 kB │ gzip:  20.93 kB   # HTTP 请求层，见下方说明
index-*.css           2.44 kB │ gzip:   0.98 kB   # 全局样式与设计令牌

# ==============================================
# 路由级懒加载 chunk（按需加载，首屏不下载）
# ==============================================
TodoListView-*.js    17.30 kB │ gzip:   5.65 kB
UserListView-*.js    14.64 kB │ gzip:   5.09 kB
DashboardView-*.js    9.24 kB │ gzip:   3.28 kB
DefaultLayout-*.js    4.06 kB │ gzip:   1.86 kB
NotFoundView-*.js     0.85 kB │ gzip:   0.60 kB

# ==============================================
# 入口与工具 chunk
# ==============================================
index-*.js            4.10 kB │ gzip:   2.05 kB   # 应用入口
useTable-*.js         0.95 kB │ gzip:   0.55 kB
useMediaQuery-*.js    0.49 kB │ gzip:   0.28 kB
todo-*.js             0.48 kB │ gzip:   0.24 kB
```

```text
# ==============================================
# 重要：format-*.js 的文件名具有误导性
# ==============================================
疑问：src/utils/format.ts 源码仅百余行，为何该 chunk 达 54.95 kB？

真相：经产物符号扫描确认，该 chunk 实际是【HTTP 请求层】，
      并不包含 format.ts 的内容（其中查无 formatDateTime、
      describeDueDate、TODO_STATUS_OPTIONS、cleanParams 等符号）

实际构成：
  ├─ axios 库本体
  │    证据：AxiosError / ERR_BAD_REQUEST / XMLHttpRequest /
  │          interceptor / FormData 等特征字符串大量出现
  ├─ src/api/request.ts
  │    证据：含 taskhub-token、silent 配置项、"网络异常" 提示文案
  └─ src/types/api.ts
       证据：含 ApiError 类
  并从 element chunk 导入 ElMessage（import{i as t}from"./element-*.js"）
  用于统一错误提示

命名原因：Rolldown（Vite 8 的打包器）按模块图中的某个模块名
          生成 chunk 文件名，与 chunk 实际内容构成并无对应关系

结论：分包结果本身是合理的——
      axios 与请求封装被三个列表视图共享，
      提取为独立 chunk 后浏览器只需下载并解析一次

教训：排查产物体积时不能凭 chunk 文件名推断内容，
      应通过符号扫描或 source-map-explorer 等工具确认
```

```text
# ==============================================
# Vite 手动分包说明（vite.config.ts）
# ==============================================
注意：Vite 8 的 manualChunks 类型只接受【函数形式】
      对象形式（{ vue: [...], element: [...] }）已不被类型支持
      使用对象形式会导致 vue-tsc 报 TS2769

策略：按模块路径判断所属依赖
      ├─ 路径含 element-plus → 'element' chunk
      ├─ 路径匹配 vue/@vue/vue-router/pinia → 'vue' chunk
      └─ 其余 node_modules 模块 → 交回 Vite 默认策略

不做「全部塞进 vendor」的粗粒度拆分
原因：会破坏按需加载与模块初始化顺序
```

## 八、生产部署

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[8.1 后端打包与启动](#81-后端打包与启动) | [8.2 前端打包](#82-前端打包) | [8.3 Nginx 配置](#83-nginx-配置)

### 8.1 后端打包与启动

> [⬆ 返回本章导航](#八生产部署) | [⬆ 返回目录导航](#目录导航)

```shell
#!/bin/bash
# ============================================================================
# 脚本名称: deploy-backend.sh（示例命令，可直接复制执行）
# 功能描述: 打包并以生产配置启动 TaskHub 后端服务
# 使用场景: 生产环境部署、预发布环境验证
# 使用方法:
#   ./deploy-backend.sh
# 前置条件:
#   1. JDK 17 已安装
#   2. Maven 3.8+ 已安装
#   3. 已按下方环境变量清单完成配置
# 注意事项:
#   1. -DskipTests 跳过测试以加快打包，CI 环境中不建议跳过
#   2. 生产环境必须关闭演示数据与接口文档
#   3. CORS 白名单必须收紧为真实前端域名
# 作者: 刘武贵
# 创建时间: 2026-09-13
# ============================================================================

set -e  # 遇到错误立即退出

# ------------------------------------------
# 1. 打包
# ------------------------------------------
cd backend
mvn clean package -DskipTests

# ------------------------------------------
# 2. 配置生产环境变量
# ------------------------------------------
export SPRING_PROFILES_ACTIVE=prod                      # 激活生产 profile
export SERVER_PORT=8080                                 # 服务端口
export APP_SEED_DATA=false                              # 关闭演示数据
export APP_CORS_ALLOWED_ORIGINS=https://your.domain     # 收紧 CORS 白名单

# ------------------------------------------
# 3. 启动（生产建议交由 systemd / Docker 托管）
# ------------------------------------------
java -jar target/taskhub-backend.jar
```

### 8.2 前端打包

> [⬆ 返回本章导航](#八生产部署) | [⬆ 返回目录导航](#目录导航)

```shell
#!/bin/bash
# ============================================================================
# 脚本名称: deploy-frontend.sh（示例命令，可直接复制执行）
# 功能描述: 构建 TaskHub 前端静态资源
# 使用场景: 生产环境部署、CDN 资源发布
# 前置条件:
#   1. Node.js 20.19+ 已安装
#   2. .env.production 中的 VITE_API_BASE_URL 已确认
# 注意事项:
#   1. 使用 npm ci 而非 npm install
#      按 package-lock.json 精确安装，保证可复现构建
#   2. 产物为纯静态文件，可部署到任意静态服务器或 CDN
#   3. 默认不输出 sourcemap
#      需要排查线上问题时把 VITE_BUILD_SOURCEMAP 置为 true
# 作者: 刘武贵
# 创建时间: 2026-09-13
# ============================================================================

set -e

cd frontend

# 按锁定文件精确安装依赖
npm ci

# 构建生产包，产物输出到 dist/
npm run build
```

```properties
# ==============================================
# 生产环境配置文件
# 文件路径: frontend/.env.production
# 加载时机: vite build 时
# 注意: 只有以 VITE_ 开头的变量会被注入到客户端代码中
# ==============================================

# 页面标题（浏览器标签页文字）
VITE_APP_TITLE=TaskHub

# 接口基础路径：生产由 Nginx 反代到后端，保持同域相对路径以规避跨域
VITE_API_BASE_URL=/api/v1

# 代理目标：生产环境不存在 Vite 代理，该变量仅用于本地 npm run preview 调试
VITE_PROXY_TARGET=http://localhost:8080

# Swagger UI 地址：留空则隐藏顶栏「接口文档」快捷入口
VITE_SWAGGER_URL=

# 是否输出 sourcemap：排查线上问题时可临时改为 true（会增大产物体积并暴露源码）
VITE_BUILD_SOURCEMAP=false
```

开发环境变量（`frontend/.env.development`）与之对照：

```properties
# ==============================================
# 开发环境配置文件
# 文件路径: frontend/.env.development
# 加载时机: vite dev 时
# 注意: 修改后必须重启 dev server，env 变更不会热更新
# ==============================================

# 页面标题（带环境后缀，便于区分当前所处环境）
VITE_APP_TITLE=TaskHub · 开发环境

# 接口基础路径：走 Vite 代理，避免浏览器跨域
VITE_API_BASE_URL=/api/v1

# Vite 开发服务器的代理目标（即后端地址）
VITE_PROXY_TARGET=http://localhost:8080

# 开发服务器端口（与 strictPort: true 配合，冲突时直接报错退出）
VITE_PORT=5173

# 后端 Swagger UI 地址，供页面顶栏快捷跳转
VITE_SWAGGER_URL=http://localhost:8080/swagger-ui.html
```

```text
# ==============================================
# 两个环境的差异对照
# ==============================================
变量                    开发环境                              生产环境
VITE_APP_TITLE          TaskHub · 开发环境                     TaskHub
VITE_API_BASE_URL       /api/v1（走代理）                      /api/v1（走 Nginx）
VITE_PROXY_TARGET       http://localhost:8080（生效）           保留但仅 preview 用
VITE_PORT               5173                                   不配置（构建无关）
VITE_SWAGGER_URL        http://localhost:8080/swagger-ui.html   空（隐藏入口）
VITE_BUILD_SOURCEMAP    不配置（dev 无需）                      false
```

### 8.3 Nginx 配置

> [⬆ 返回本章导航](#八生产部署) | [⬆ 返回目录导航](#目录导航)

前端使用 History 路由模式，**必须**把所有未命中的路径回退到 `index.html`；同时把 `/api` 反向代理到后端以避免跨域：

```nginx
# =============================================
# taskhub.conf - TaskHub 生产环境 Nginx 配置
# =============================================
# 功能描述: 托管前端静态资源并反向代理后端 API
# 适用场景: 单节点部署、前后端同域部署
#
# 配置要点:
#   1. History 路由必须回退到 index.html，否则刷新页面 404
#   2. assets 目录文件名带 hash，可放心设置长缓存
#   3. /api 反向代理到后端，实现前后端同域，规避跨域
#   4. 生产环境关闭接口文档，避免接口结构对外暴露
#
# 使用方法:
#   放置到 /etc/nginx/conf.d/taskhub.conf
#   执行 nginx -t 校验后 nginx -s reload 生效
#
# 作者: 刘武贵
# 创建时间: 2026-09-13
# =============================================

server {
    listen       80;                        # 监听端口
    server_name  your.domain.com;           # 站点域名

    root   /var/www/taskhub/dist;           # 前端构建产物目录
    index  index.html;                      # 默认首页

    # ------------------------------------------
    # 前端 History 路由回退
    # ------------------------------------------
    location / {
        try_files $uri $uri/ /index.html;   # 未命中文件则回退到 index.html
    }

    # ------------------------------------------
    # 静态资源长缓存（文件名带 hash，可放心缓存）
    # ------------------------------------------
    location /assets/ {
        expires 1y;                                       # 缓存一年
        add_header Cache-Control "public, immutable";     # 标记为不可变
    }

    # ------------------------------------------
    # API 反向代理到后端
    # ------------------------------------------
    location /api/ {
        proxy_pass         http://127.0.0.1:8080;           # 后端地址
        proxy_set_header   Host              $host;         # 透传原始 Host
        proxy_set_header   X-Real-IP         $remote_addr;  # 透传客户端 IP
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;  # 代理链
        proxy_set_header   X-Forwarded-Proto $scheme;       # 原始协议
    }

    # ------------------------------------------
    # 生产环境关闭接口文档
    # ------------------------------------------
    location ~ ^/(swagger-ui|v3/api-docs) {
        return 404;                        # 直接返回 404
    }
}
```

> 上述 Nginx 规则是**第二道防线**。第一道防线已在应用层落地：`application-prod.yml` 中 `springdoc.api-docs.enabled` 与 `springdoc.swagger-ui.enabled` 均已写死为 `false`，激活 `prod` profile 后文档端点本身就不存在，无需额外设置。
>
> 若因排障需要临时开启，应通过启动参数覆盖（`--springdoc.api-docs.enabled=true`）而非修改配置文件，并在排障结束后立即移除，避免文档端点长期暴露。

## 九、常见问题

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[9.1 启动与连通性问题](#91-启动与连通性问题) | [9.2 数据与配置问题](#92-数据与配置问题) | [9.3 扩展与演进问题](#93-扩展与演进问题)

### 9.1 启动与连通性问题

> [⬆ 返回本章导航](#九常见问题) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 问题一：前端页面能打开，但数据全是空的
#         或提示「网络异常，无法连接到后端服务」
# ==============================================
原因：后端没启动，或端口不是 8080
排查：curl http://localhost:8080/actuator/health
      返回 {"status":"UP"} 才正常
解决：若后端在其他端口，修改 frontend/.env.development 的 VITE_PROXY_TARGET
      然后【重启】npm run dev（env 文件变更不会热更新）

# ==============================================
# 问题二：npm run dev 报端口被占用
# ==============================================
原因：vite.config.ts 里设置了 strictPort: true
      端口冲突时会直接报错退出而不是静默换端口
说明：这是刻意设计，避免前后端代理配置错位
解决：释放 5173 端口，或改 .env.development 里的 VITE_PORT

# ==============================================
# 问题三：后端启动报 Port 8080 was already in use
# ==============================================
解决：mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
      同时把前端的 VITE_PROXY_TARGET 改成 8081

# ==============================================
# 问题四：后台启动的 dev server 无响应，curl 全部超时
# ==============================================
原因：Vite 会读取 stdin 以响应「press h + enter」交互提示
      用 nohup npm run dev > log 2>&1 & 启动时
      进程会因尝试读取 tty 输入被 SIGTTIN 信号挂起
解决：启动时重定向标准输入 → nohup npm run dev < /dev/null > log 2>&1 &
清理：lsof -ti tcp:5173 | while read -r p; do kill -9 "$p"; done
```

### 9.2 数据与配置问题

> [⬆ 返回本章导航](#九常见问题) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 问题五：H2 是内存库，重启后数据就没了
# ==============================================
说明：仅 dev / test 两个 profile 是内存库，
      内存库随进程退出即清空，属刻意设计，便于反复演示
prod 现状：application-prod.yml 已使用【文件模式】，数据可持久化
        url: jdbc:h2:file:./data/taskhub;AUTO_SERVER=TRUE;MODE=MySQL
        数据落在启动目录下的 ./data/taskhub.mv.db
dev 若要持久化：把 application-dev.yml 的 JDBC URL 同样改为 file 模式
        注意 ddl-auto 在 dev 下是 create-drop，
        即使改成文件模式，重启时仍会先删表再重建，
        需同时把 ddl-auto 调整为 update 才能真正保住数据

# ==============================================
# 问题六：响应体里为什么没有值为 null 的字段
# ==============================================
原因：全局配置了 Jackson default-property-inclusion: non_null
影响：值为 null 的字段不会出现在 JSON 中
应对：前端类型里这些字段统一声明为可选（field?: T）
重要例外：最外层 ApiResponse 带类级别 @JsonInclude(ALWAYS)，
        因此 code / message / data / timestamp 四个字段恒定输出，
        data 为 null 时也会显式给出 "data": null，
        前端无需为 data 做「字段可能不存在」的兼容
范围界定：例外只作用于 ApiResponse 自身这一层，
        data 内部的业务对象仍遵循全局 non_null，
        完整契约与实测报文见本文档 4.3 节

# ==============================================
# 问题七：时间显示比录入时间差 8 小时
# ==============================================
原因：用 new Date() 解析不带时区后缀的 ISO 字符串，
      不同浏览器时区处理不一致
说明：本项目 src/utils/format.ts 已采用纯字符串解析规避此问题
提醒：新增时间展示逻辑时应复用 format.ts，不要直接 new Date()
```

### 9.3 扩展与演进问题

> [⬆ 返回本章导航](#九常见问题) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 问题八：想接入真实数据库（MySQL / PostgreSQL）
# ==============================================
步骤一：pom.xml 加入对应驱动依赖
步骤二：新建 application-mysql.yml，配置 spring.datasource.*
        与 spring.jpa.database-platform
步骤三：生产 profile 下把 ddl-auto 设为 validate，
        并引入 Flyway / Liquibase 管理表结构
        注意：当前 application-prod.yml 的实际值是 update，
        validate 是接入真实数据库后的【建议改进方向】，
        update 会自动改表结构，在生产环境有数据丢失风险
注意：user 在多数数据库中是保留字，
      本项目实体表名已用 sys_user 规避

# ==============================================
# 问题九：为什么 Controller 路径直接写 /api/v1/...
#         而不是配置 server.servlet.context-path
# ==============================================
原因：context-path 会同时影响 Swagger UI、Actuator、H2 控制台
      等所有端点的路径，本地调试与代理配置都容易踩坑
收益：显式写在 @RequestMapping 上更直观，
      也让 MockMvc 测试路径与真实路径完全一致

# ==============================================
# 问题十：前端如何按需引入 Element Plus 以减小包体积
# ==============================================
现状：为了示例可读性采用全量引入（gzip 后 353.55 kB）
方案：安装 unplugin-vue-components 与 unplugin-auto-import，
      在 vite.config.ts 中配置 ElementPlusResolver 实现自动按需导入，
      同时去掉 main.ts 里的全量 app.use(ElementPlus) 与图标全局注册

# ==============================================
# 问题十一：这个示例有登录鉴权吗
# ==============================================
现状：没有。项目聚焦于分层结构与工程规范，
      未引入 Spring Security，所有接口无鉴权
预留：src/api/request.ts 中已预留 Token 读取与 Authorization 头注入逻辑
接入：登录后把 Token 写入 localStorage 的 taskhub-token 即可自动携带

# ==============================================
# 问题十二：el-link 组件出现弃用告警
# ==============================================
告警：The underline option (boolean) is about to be deprecated
      in version 3.0.0, please use 'always' | 'hover' | 'never' instead
原因：Element Plus 2.14 起 :underline="false" 布尔写法已弃用
解决：改为字符串枚举写法 underline="never"
```

## 十、维护记录

> [⬆ 返回目录导航](#目录导航)

> **本章导航**：[10.1 文档信息与版本历史](#101-文档信息与版本历史) | [10.2 交付物核对清单](#102-交付物核对清单)

### 10.1 文档信息与版本历史

> [⬆ 返回本章导航](#十维护记录) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 文档信息
# ==============================================
文档名称: TaskHub.md
功能描述: TaskHub 全栈项目完整说明文档
适用范围: 项目 v1.0
作者: 刘武贵
创建时间: 2026-09-13
编写依据: 《Java 项目注释规范指南 v1.0》（注释规范.md）
内容基准: 全部技术细节以源码与运行时实测为准，
          而非沿用原 README.md 的描述
```

| 版本 | 日期 | 修改人 | 修改内容 |
| --- | --- | --- | --- |
| v1.0 | 2026-09-13 | 刘武贵 | 首次编写，按《Java 项目注释规范指南 v1.0》风格重组原 README.md 内容 |

### 10.2 交付物核对清单

> [⬆ 返回本章导航](#十维护记录) | [⬆ 返回目录导航](#目录导航)

```text
# ==============================================
# 项目交付物核对清单
# ==============================================
后端
  [✓] Spring Boot 3.5.9 + Java 17
  [✓] 53 个 Java 源文件（49 主代码 + 4 测试类）
  [✓] 16 个 REST 端点（待办 8 + 用户 8）
  [✓] H2 数据库（dev/test 内存模式、prod 文件模式）+ JPA 自动建表
  [✓] Swagger UI + OpenAPI 3.1 文档（prod 已关闭）
  [✓] 全局异常处理 + 5 位错误码体系（15 个码位）
  [✓] 58 个测试用例全部通过
  [✓] 3 套 Profile 配置（dev / prod / test）

前端
  [✓] Vue 3.5 + TypeScript 5.9 + Vite 8.3
  [✓] 27 个源文件（含 10 个 .vue 组件）
  [✓] Element Plus 2.14 全量集成 + 中文语言包
  [✓] Axios 统一封装 + 类型安全 http 对象
  [✓] 4 个页面（仪表盘 / 待办 / 用户 / 404）
  [✓] 响应式布局（4 级断点 + 移动端抽屉）
  [✓] vue-tsc 类型检查零错误
  [✓] 生产构建成功（1699 模块）
  [✓] 浏览器实测零 console error

文档
  [✓] README.md（项目简介）
  [✓] TaskHub.md（本文档，完整说明）
  [✓] .editorconfig / .gitignore
```

> [⬆ 返回目录导航](#目录导航)

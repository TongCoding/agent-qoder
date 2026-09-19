# TaskHub · 前后端分离全栈示例

一个可直接运行的全栈脚手架项目，后端使用 **Spring Boot 3 + Java 17**，前端使用 **Vue 3 + TypeScript + Element Plus**。
业务上同时实现了「待办事项」与「用户管理」两个模块，覆盖了后台管理系统中最常见的**分页筛选、服务端排序、一对多关联、状态流转、聚合统计、参数校验与全局异常处理**等场景。

> 项目定位为**工程规范示例**：不追求业务复杂度，而是把分层结构、统一契约、错误处理、测试与构建配置这些「容易被省略但决定项目可维护性」的部分做扎实。

---

## 目录

- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [接口文档](#接口文档)
- [后端设计说明](#后端设计说明)
- [前端设计说明](#前端设计说明)
- [测试](#测试)
- [生产部署](#生产部署)
- [常见问题](#常见问题)

---

## 技术栈

### 后端

| 分类 | 选型 | 版本 | 说明 |
| --- | --- | --- | --- |
| 语言 | Java | 17 | LTS 版本，启用了 Records、`switch` 表达式等特性 |
| 框架 | Spring Boot | 3.5.9 | Web MVC + Spring Data JPA |
| 数据库 | H2 | 2.3.x（随 Boot 管理） | 内存模式，启动即用，无需安装 |
| ORM | Hibernate | 6.x（随 Boot 管理） | 使用 Criteria API / Specification 做动态查询 |
| 接口文档 | springdoc-openapi | 2.8.17 | 生成 OpenAPI 3.1 文档并内嵌 Swagger UI |
| 参数校验 | Jakarta Bean Validation | 3.0 | `@Valid` + 注解约束 |
| 监控 | Spring Boot Actuator | 随 Boot | 健康检查端点 |
| 简化代码 | Lombok | 随 Boot | `@Data` / `@Builder` / `@RequiredArgsConstructor` |
| 测试 | JUnit 5 + MockMvc + Mockito + AssertJ | 随 Boot | 58 个用例 |
| 构建 | Maven | 3.8+ | `spring-boot-maven-plugin` 打可执行 jar |

### 前端

| 分类 | 选型 | 版本 | 说明 |
| --- | --- | --- | --- |
| 框架 | Vue | 3.5.x | 全量使用 `<script setup>` Composition API |
| 语言 | TypeScript | 5.9.x | `strict` 模式 + `noUnusedLocals` |
| 构建 | Vite | 8.3.x | 开发代理 + 生产打包与手动分包 |
| UI | Element Plus | 2.14.x | 中文语言包 + 全量图标注册 |
| HTTP | Axios | 1.20.x | 统一实例、拦截器、类型安全的请求封装 |
| 路由 | Vue Router | 4.6.x | History 模式 + 路由懒加载 |
| 状态 | Pinia | 3.0.x | 仅存放跨页面共享的 UI 状态 |
| 样式 | Sass | 1.104.x | CSS 自定义属性做设计令牌 + 组件内 scoped 嵌套 |

---

## 快速开始

### 环境要求

| 工具 | 最低版本 | 检查命令 |
| --- | --- | --- |
| JDK | 17 | `java -version` |
| Maven | 3.8 | `mvn -v` |
| Node.js | 20.19 | `node -v` |
| npm | 10 | `npm -v` |

> 前端 `package.json` 中通过 `engines` 字段声明了 Node 版本要求，Vite 8 不支持低于 20.19 的 Node。

### 一、启动后端

```bash
cd backend

# 开发模式启动（默认 dev profile，H2 内存库 + 自动灌入演示数据）
mvn spring-boot:run
```

看到下面这行即表示启动成功：

```
Started TaskHubApplication in x.xxx seconds
演示数据初始化完成 | 用户 6 条 | 待办 13 条
```

后端默认监听 **http://localhost:8080**，可访问：

| 地址 | 说明 |
| --- | --- |
| http://localhost:8080/swagger-ui.html | Swagger UI 接口文档（可直接在线调试） |
| http://localhost:8080/v3/api-docs | OpenAPI 3.1 JSON |
| http://localhost:8080/actuator/health | 健康检查 |
| http://localhost:8080/h2-console | H2 控制台（JDBC URL 见下方说明） |

> **H2 控制台连接参数**：JDBC URL 填 `jdbc:h2:mem:taskhub`，User Name 填 `sa`，Password 留空。

也可以用打好的 jar 启动：

```bash
cd backend
mvn clean package
java -jar target/taskhub-backend.jar
```

### 二、启动前端

```bash
cd frontend

# 首次运行需要安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认监听 **http://localhost:5173**，浏览器打开即可看到仪表盘。

开发环境下，前端所有 `/api/**` 请求都会由 Vite 代理转发到 `http://localhost:8080`（见 `vite.config.ts` 的 `server.proxy`），因此**不存在浏览器跨域问题**。后端地址可通过 `frontend/.env.development` 中的 `VITE_PROXY_TARGET` 修改。

### 三、验证是否跑通

```bash
# 待办分页（page 从 1 开始）
curl "http://localhost:8080/api/v1/todos?page=1&size=3"

# 待办统计
curl "http://localhost:8080/api/v1/todos/statistics"

# 用户下拉选项
curl "http://localhost:8080/api/v1/users/options"

# 创建一条待办
curl -X POST "http://localhost:8080/api/v1/todos" \
  -H 'Content-Type: application/json' \
  -d '{"title":"测试任务","priority":"HIGH","dueDate":"2026-12-31","assigneeId":2}'

# 触发参数校验失败（返回 code=40001 与字段级明细）
curl -X POST "http://localhost:8080/api/v1/todos" \
  -H 'Content-Type: application/json' \
  -d '{"title":""}'
```

---

## 项目结构

```
agent-qoder/
├── backend/                                 # Spring Boot 后端
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── java/com/example/taskhub
│       │   │   ├── TaskHubApplication.java   # 启动类
│       │   │   ├── common/                   # 通用契约
│       │   │   │   ├── ApiResponse.java      #   统一响应体
│       │   │   │   ├── ErrorCode.java        #   业务错误码枚举
│       │   │   │   ├── FieldValidationError.java
│       │   │   │   ├── PageQuery.java        #   分页请求基类（1-based）
│       │   │   │   └── PageResult.java       #   分页响应体
│       │   │   ├── config/
│       │   │   │   ├── AppProperties.java    #   app.* 自定义配置绑定
│       │   │   │   ├── DataInitializer.java  #   演示数据灌入
│       │   │   │   ├── OpenApiConfig.java    #   Swagger 文档信息
│       │   │   │   └── WebConfig.java        #   CORS / Jackson 等 Web 配置
│       │   │   ├── controller/               # 接口层，只做参数绑定与响应包装
│       │   │   │   ├── TodoController.java
│       │   │   │   └── UserController.java
│       │   │   ├── dto/                      # 请求 / 响应对象（Java Record）
│       │   │   │   ├── todo/
│       │   │   │   └── user/
│       │   │   ├── entity/                   # JPA 实体
│       │   │   │   ├── BaseEntity.java       #   id / createdAt / updatedAt
│       │   │   │   ├── TodoItem.java
│       │   │   │   └── User.java
│       │   │   ├── enums/                    # 业务枚举
│       │   │   ├── exception/
│       │   │   │   ├── BusinessException.java
│       │   │   │   ├── ErrorCode 相关
│       │   │   │   └── GlobalExceptionHandler.java  # 全局异常捕获
│       │   │   ├── mapper/                   # Entity ↔ DTO 手动转换
│       │   │   ├── repository/               # Spring Data JPA
│       │   │   │   ├── projection/           #   接口投影（聚合统计）
│       │   │   │   └── specification/        #   动态查询条件
│       │   │   └── service/                  # 业务层
│       │   │       ├── TodoService.java / impl/
│       │   │       └── UserService.java / impl/
│       │   └── resources/
│       │       ├── application.yml           # 公共配置
│       │       ├── application-dev.yml       # 开发环境（H2 内存 + SQL 日志）
│       │       └── application-prod.yml      # 生产环境
│       └── test
│           ├── java/com/example/taskhub/
│           │   ├── TaskHubApplicationTests.java
│           │   ├── controller/               # MockMvc 集成测试
│           │   │   ├── TodoApiIntegrationTest.java
│           │   │   └── UserApiIntegrationTest.java
│           │   └── service/impl/             # Mockito 单元测试
│           │       └── TodoServiceImplTest.java
│           └── resources/application-test.yml
│
├── frontend/                                # Vue 3 前端
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   ├── .env.development                     # 开发环境变量
│   ├── .env.production                      # 生产环境变量
│   ├── public/favicon.svg
│   └── src
│       ├── main.ts                          # 入口：注册 Element Plus / Pinia / Router
│       ├── App.vue
│       ├── env.d.ts                         # import.meta.env 类型声明
│       ├── api/
│       │   ├── request.ts                   # Axios 实例 + 拦截器 + http 封装
│       │   ├── todo.ts                      # 待办接口
│       │   └── user.ts                      # 用户接口
│       ├── types/
│       │   ├── api.ts                       # ApiResponse / PageResult / ApiError
│       │   ├── todo.ts
│       │   └── user.ts
│       ├── composables/
│       │   ├── useTable.ts                  # 分页表格通用逻辑（含请求竞态处理）
│       │   ├── useUserOptions.ts            # 负责人下拉数据
│       │   └── useMediaQuery.ts             # 响应式断点
│       ├── stores/app.ts                    # Pinia：侧边栏折叠状态
│       ├── router/index.ts                  # 路由表（菜单由此派生）
│       ├── styles/index.scss                # 设计令牌 + 全局工具类 + 响应式断点
│       ├── utils/
│       │   ├── dict.ts                      # 枚举字典（label / tag 颜色）
│       │   ├── format.ts                    # 日期时间格式化（纯字符串解析，无时区漂移）
│       │   └── params.ts                    # 查询参数清洗
│       ├── layouts/
│       │   ├── DefaultLayout.vue            # 侧边栏 + 顶栏 + 内容区
│       │   └── SideMenu.vue                 # 菜单（桌面侧栏与移动抽屉共用）
│       ├── components/
│       │   ├── common/StatCard.vue
│       │   ├── todo/TodoFormDialog.vue
│       │   └── user/UserFormDialog.vue
│       └── views/
│           ├── DashboardView.vue            # 仪表盘（聚合统计 + 分布图 + 最近待办）
│           ├── TodoListView.vue             # 待办列表（筛选/排序/分页/流转/批量删除）
│           ├── UserListView.vue             # 用户列表
│           └── NotFoundView.vue             # 404
│
├── .editorconfig
├── .gitignore
└── README.md
```

---

## 接口文档

启动后端后打开 **http://localhost:8080/swagger-ui.html**，可直接在线调试全部接口。
前端顶栏右侧也提供了「接口文档」快捷入口（地址由 `VITE_SWAGGER_URL` 配置，生产环境默认为空即隐藏）。

### 接口清单

**待办事项 `/api/v1/todos`**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/todos` | 分页查询，支持关键字 / 状态 / 优先级 / 负责人 / 未分配 / 逾期 / 截止日期区间组合筛选 |
| GET | `/api/v1/todos/{id}` | 查询详情 |
| GET | `/api/v1/todos/statistics` | 聚合统计（仪表盘一次请求拿全部指标） |
| POST | `/api/v1/todos` | 创建（返回 201） |
| PUT | `/api/v1/todos/{id}` | 全量更新 |
| PATCH | `/api/v1/todos/{id}/status` | 仅流转状态，转为 `DONE` 时自动写入 `completedAt` |
| DELETE | `/api/v1/todos/{id}` | 删除单条 |
| DELETE | `/api/v1/todos?ids=1,2,3` | 批量删除（存在无效 ID 则整体失败） |

**用户管理 `/api/v1/users`**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/users` | 分页查询，支持关键字 / 角色 / 状态 / 部门筛选 |
| GET | `/api/v1/users/{id}` | 查询详情 |
| GET | `/api/v1/users/options` | 下拉选项（仅 `ACTIVE` 用户），用于待办的负责人选择器 |
| GET | `/api/v1/users/statistics` | 总数 + 角色分布 + 状态分布 |
| POST | `/api/v1/users` | 创建（返回 201） |
| PUT | `/api/v1/users/{id}` | 全量更新（用户名不可改） |
| PATCH | `/api/v1/users/{id}/status` | 变更账号状态 |
| DELETE | `/api/v1/users/{id}` | 删除；名下仍有待办时返回 `40903` |

---

## 后端设计说明

### 1. 统一响应契约

所有接口（**包括异常响应**）都返回同一结构，前端只需一套解析逻辑：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": { },
  "timestamp": "2026-09-13T17:47:11.225090"
}
```

- `code = 0` 表示成功，非 0 为业务错误码；
- 时间为 **ISO-8601** 格式（`write-dates-as-timestamps: false`），前端可直接解析；
- 全局配置了 `default-property-inclusion: non_null`，值为 `null` 的字段不会出现在 JSON 中，减小报文体积。因此前端类型里这些字段统一声明为**可选**。

### 2. 错误码设计

错误码为 5 位数字，**前 3 位与 HTTP 状态码对齐**，便于快速定位问题层级：

| 错误码 | HTTP | 含义 |
| --- | --- | --- |
| `0` | 200 | 成功 |
| `40001` | 400 | 参数校验失败（`data` 为字段级错误明细数组） |
| `40002` | 400 | 请求参数非法 |
| `40003` | 400 | 请求体不可读（JSON 格式错误等） |
| `40401` | 404 | 待办不存在 |
| `40402` | 404 | 用户不存在 |
| `40501` | 405 | 请求方法不支持 |
| `40901` | 409 | 用户名已存在 |
| `40902` | 409 | 邮箱已存在 |
| `40903` | 409 | 资源被引用，无法删除 |
| `50001` | 500 | 服务器内部错误 |

参数校验失败时，`data` 会返回**字段级明细**，前端据此把错误直接标注到对应表单项上：

```json
{
  "code": 40001,
  "message": "标题不能为空",
  "data": [
    { "field": "title", "message": "标题不能为空", "rejectedValue": "" }
  ]
}
```

### 3. 全局异常处理

`GlobalExceptionHandler`（`@RestControllerAdvice`）集中兜底，Controller 内**不需要写任何 try-catch**：

- `BusinessException` → 按业务错误码返回；
- `MethodArgumentNotValidException` / `BindException` → `40001` + 字段明细；
- `ConstraintViolationException` → `40002`；
- `HttpMessageNotReadableException` → `40003`（JSON 解析失败）；
- `MethodArgumentTypeMismatchException` → `40002`（例如枚举值非法）；
- `HttpRequestMethodNotSupportedException` → `40501`；
- `NoResourceFoundException` → 404；
- `DataIntegrityViolationException` → `40903`（唯一约束冲突等）；
- `Exception` → `50001`，**只记录完整堆栈到日志，响应体不泄漏内部细节**。

### 4. 分页约定

对外 API 的 `page` 从 **1** 开始（与 Element Plus 分页组件语义一致），内部由 `PageQuery.toPageable()` 转换为 Spring Data 的 0-based 页码；响应的 `PageResult.page` 同样回写为 1-based。前后端无需各自记住一套偏移规则。

`size` 上限为 100，超出会被截断，防止一次请求拖垮数据库。

### 5. 排序字段白名单

`sortBy` 直接来自前端，若原样拼进 `Sort.by()` 会在遇到非法字段时抛出 `PropertyReferenceException`（500）。
因此 `PageQuery.resolveSortField()` 会用各查询 DTO 声明的白名单做校验，**未命中时静默回退到默认排序字段**，既不报错也不会被注入。

```java
// TodoQueryRequest
public static final String[] SORTABLE_FIELDS = {
    "id", "title", "status", "priority", "dueDate", "completedAt", "createdAt", "updatedAt"
};
```

### 6. 避免 N+1 查询

两个典型处理：

- **待办列表的负责人**：`TodoSpecifications` 中按需追加 `LEFT JOIN FETCH`，并且**在 count 查询时跳过 fetch**（count 查询带 fetch 会被 Hibernate 忽略并告警）；
- **用户列表的待办数量**：`UserServiceImpl.loadTodoCounts()` 用一次 `GROUP BY assignee_id` 聚合查询拿到当前页所有用户的计数，再在内存中填充，而不是每行触发一次 count。

### 7. 分层职责

```
Controller  →  只做参数绑定、校验触发、响应包装，不写业务逻辑
Service     →  业务规则、事务边界、跨 Repository 编排
Repository  →  数据访问，复杂条件交给 Specification
Mapper      →  Entity ↔ DTO 转换（手写，无 MapStruct 依赖，便于调试）
DTO         →  Record（请求/响应）+ Lombok 类（查询条件，需可变以支持 @ModelAttribute 绑定）
```

实体不直接对外暴露，避免懒加载代理序列化异常与字段泄漏。

### 8. 演示数据

`DataInitializer` 是一个 `ApplicationRunner`，受 `app.seed-data` 开关控制（默认开启），启动时灌入：

- **6 个用户**：覆盖 4 种角色与启用/禁用状态；
- **13 条待办**：刻意覆盖逾期、今日到期、进行中、待处理、已完成、已取消、未分配等各种边界情况，方便直接看到筛选与统计效果。其中包含一条标题为「编写项目周报」的数据，与 Swagger 中 `keyword` 参数的示例值一致，可直接用来验证关键字搜索。

生产环境请通过 `APP_SEED_DATA=false` 关闭。

### 9. 多环境配置

| Profile | 用途 | 特点 |
| --- | --- | --- |
| `dev`（默认） | 本地开发 | H2 内存库、打印 SQL、`ddl-auto: update`、开启演示数据 |
| `prod` | 生产 | 关闭 SQL 日志与演示数据，CORS 白名单收紧，`ddl-auto: validate` |
| `test` | 测试 | 独立 H2 库，`ddl-auto: create-drop`，关闭 springdoc |

切换方式：`java -jar app.jar --spring.profiles.active=prod`，或设置环境变量 `SPRING_PROFILES_ACTIVE=prod`。

---

## 前端设计说明

### 1. Axios 封装

`src/api/request.ts` 做了四件事：

1. **统一实例**：`baseURL` 取自 `VITE_API_BASE_URL`（`/api/v1`），超时 15s，默认 JSON 头；
2. **响应解包**：拦截器校验 `code !== 0` 时抛出 `ApiError`，成功时把 `body.data` 提升到 `response.data`，业务代码拿到的直接是数据本身；
3. **统一错误提示**：`ElMessage.error` 自动弹出，并对 1 秒内的重复提示做抑制，避免批量请求同时失败时刷屏；
4. **可按需静默**：扩展了 `AxiosRequestConfig.silent`，表单提交类请求设为 `silent: true`，把服务端的字段级错误回填到具体表单项上，而不是弹一个笼统的 Toast。

上层通过类型安全的 `http` 对象调用，泛型即业务数据类型，无需手动断言：

```ts
const page = await http.get<PageResult<TodoItem>>('/todos', { params })
```

`ApiError` 携带 `code`、`httpStatus` 与 `details`，并提供 `fieldMessage('title')` 便捷方法。

### 2. 表格逻辑复用

`src/composables/useTable.ts` 把每个列表页都会重复的样板逻辑收敛到一处：

```ts
const { loading, list, total, query, search, reset,
        handleCurrentChange, handleSizeChange, handleSortChange } = useTable({
  fetcher: pageTodos,
  defaultQuery: DEFAULT_QUERY,
})
```

除常规的加载态与分页外，还处理了两个容易踩的坑：

- **请求竞态**：内部维护自增请求序号，只采用最后一次请求的响应，避免慢请求覆盖新数据；
- **排序映射**：把 el-table 的 `ascending / descending` 转成后端的 `asc / desc`，取消排序时清空 `sortBy` 交回后端默认字段。

### 3. 枚举字典

后端是 Java 枚举，前端在 `src/utils/dict.ts` 维护一份对应的中文标签与 `el-tag` 颜色映射。所有下拉选项、标签渲染都从这里取值，避免文案散落在各个组件中不一致。
其中 `nextStatusOptions()` 还定义了**合理的状态流转路径**，行内操作只会展示有效的下一步。

### 4. 日期格式化

后端输出的 `LocalDateTime` 形如 `2026-09-13T10:30:00`（不带时区后缀），交给 `new Date()` 解析在不同浏览器下时区处理并不一致，容易出现「显示时间比录入时间差 8 小时」。
`src/utils/format.ts` 因此采用**纯字符串解析**，所见即所得，不做任何时区换算。

### 5. 查询参数清洗

Element Plus 控件清空后会把值置为 `''` 或 `null`，直接发送会让后端收到 `status=` 这类脏参数并在枚举绑定时抛 400。
`cleanParams()` 在发请求前统一剔除空值，同时**保留 `false` 与 `0`**（它们是有效业务值，例如 `overdue=false`）。

### 6. 响应式布局

| 断点 | 行为 |
| --- | --- |
| > 1200px | 侧边栏常驻，统计卡片 4 列 |
| ≤ 1200px | 内边距收紧，统计卡片降为 2 列 |
| ≤ 992px | 顶栏隐藏页面说明文字 |
| ≤ 768px | 侧边栏改为**抽屉浮层**，筛选表单单列，分页器隐藏 sizes/jumper |

断点判断由 `useMediaQuery()`（基于 `window.matchMedia`）提供，比监听 `resize` 手动比较宽度更省开销；
桌面端的侧边栏折叠状态通过 Pinia 持久化到 `localStorage`。

设计令牌（颜色、间距、圆角、阴影、过渡时长）以 **CSS 自定义属性**形式定义在 `:root`，而不是 Sass 变量 + `additionalData` 注入——前者可被运行时主题覆盖，也不会让每个组件都隐式依赖一份全局变量表。

### 7. 菜单与路由单一数据源

侧边栏菜单由 `router/index.ts` 导出的 `mainRoutes` **派生**，而非另写一份菜单配置。新增页面时只改路由表，菜单、面包屑、页面标题会自动跟上。

---

## 测试

### 后端

```bash
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

集成测试使用 `@ActiveProfiles("test")` + `@Transactional`，每个用例执行后自动回滚，互不干扰。

### 前端

```bash
cd frontend
npm run type-check   # vue-tsc 全量类型检查（strict 模式）
npm run build        # 类型检查 + 生产构建
```

---

## 生产部署

### 1. 后端打包

```bash
cd backend
mvn clean package -DskipTests
java -jar target/taskhub-backend.jar --spring.profiles.active=prod
```

生产环境需通过环境变量或命令行参数覆盖以下配置：

```bash
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=8080
export APP_SEED_DATA=false                          # 关闭演示数据
export APP_CORS_ALLOWED_ORIGINS=https://your.domain # 收紧 CORS 白名单
```

### 2. 前端打包

```bash
cd frontend
npm ci          # 按 package-lock.json 精确安装，保证可复现构建
npm run build   # 产物输出到 dist/
```

产物为纯静态文件，可部署到任意静态服务器或 CDN。

### 3. Nginx 参考配置

前端使用 History 路由模式，**必须**把所有未命中的路径回退到 `index.html`；同时把 `/api` 反向代理到后端以避免跨域：

```nginx
server {
    listen       80;
    server_name  your.domain.com;

    root   /var/www/taskhub/dist;
    index  index.html;

    # 前端 History 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源长缓存（文件名带 hash，可放心缓存）
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # API 反向代理到后端
    location /api/ {
        proxy_pass         http://127.0.0.1:8080;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }

    # 生产环境建议关闭接口文档
    location ~ ^/(swagger-ui|v3/api-docs) {
        return 404;
    }
}
```

> 生产环境务必设置 `springdoc.swagger-ui.enabled=false` 与 `springdoc.api-docs.enabled=false`，避免接口结构对外暴露。

---

## 常见问题

**Q：前端页面能打开，但数据全是空的 / 提示「网络异常，无法连接到后端服务」？**
A：后端没启动，或端口不是 8080。先 `curl http://localhost:8080/actuator/health` 确认，返回 `{"status":"UP"}` 才正常。若后端在其他端口，修改 `frontend/.env.development` 的 `VITE_PROXY_TARGET` 后**重启** `npm run dev`（env 文件变更不会热更新）。

**Q：`npm run dev` 报端口被占用？**
A：`vite.config.ts` 里设置了 `strictPort: true`，端口冲突时会直接报错退出而不是静默换端口——这是为了避免前后端代理配置错位。请释放 5173 端口，或改 `.env.development` 里的 `VITE_PORT`。

**Q：后端启动报 `Port 8080 was already in use`？**
A：`mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`，同时把前端的 `VITE_PROXY_TARGET` 改成 8081。

**Q：H2 是内存库，重启后数据就没了？**
A：这是刻意的设计，便于反复演示。若要持久化，把 `application-dev.yml` 中的 JDBC URL 改为文件模式：
`jdbc:h2:file:./data/taskhub;AUTO_SERVER=TRUE`。

**Q：想接入真实数据库（MySQL / PostgreSQL）？**
A：三步——① `pom.xml` 加入对应驱动依赖；② 新建 `application-mysql.yml` 配置 `spring.datasource.*` 与 `spring.jpa.database-platform`；③ 生产 profile 下把 `ddl-auto` 设为 `validate`，并引入 Flyway / Liquibase 管理表结构。注意 `user` 在多数数据库中是保留字，本项目实体表名已用 `sys_user` 规避。

**Q：为什么 Controller 路径直接写 `/api/v1/...`，而不是配置 `server.servlet.context-path`？**
A：`context-path` 会同时影响 Swagger UI、Actuator、H2 控制台等所有端点的路径，本地调试与代理配置都容易踩坑。显式写在 `@RequestMapping` 上更直观，也让 MockMvc 测试路径与真实路径完全一致。

**Q：前端如何按需引入 Element Plus 以减小包体积？**
A：当前为了示例的可读性采用全量引入（gzip 后约 354 KB）。生产项目可安装 `unplugin-vue-components` 与 `unplugin-auto-import`，在 `vite.config.ts` 中配置 `ElementPlusResolver` 实现自动按需导入，同时把 `main.ts` 里的全量 `app.use(ElementPlus)` 与图标全局注册去掉。

**Q：这个示例有登录鉴权吗？**
A：没有。项目聚焦于分层结构与工程规范，未引入 Spring Security，所有接口无鉴权。`src/api/request.ts` 中已预留了 Token 读取与 `Authorization` 头注入逻辑，接入登录后写入 `localStorage` 的 `taskhub-token` 即可自动携带。

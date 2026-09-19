package com.example.taskhub.config;

import com.example.taskhub.entity.TodoItem;
import com.example.taskhub.entity.User;
import com.example.taskhub.enums.TodoPriority;
import com.example.taskhub.enums.TodoStatus;
import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import com.example.taskhub.repository.TodoRepository;
import com.example.taskhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 演示数据初始化器。
 *
 * <p>应用启动后向数据库写入一批用户与待办数据，方便前端页面直接联调，
 * 无需手动调用接口造数据。通过配置项 {@code app.seed-data=false} 可关闭（生产环境默认关闭）。</p>
 *
 * <p>由于开发环境使用 H2 内存库且 {@code ddl-auto=create-drop}，每次重启都会重建表结构，
 * 因此这里的写入天然是幂等的；同时仍保留了 {@code count() > 0} 的短路判断，
 * 以兼容切换到文件数据库或 MySQL 等持久化存储的场景。</p>
 *
 * @author TaskHub
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app", name = "seed-data", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final TodoRepository todoRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("数据库中已存在数据，跳过演示数据初始化");
            return;
        }

        List<User> users = initUsers();
        initTodos(users);

        log.info("演示数据初始化完成 | 用户 {} 条 | 待办 {} 条",
                userRepository.count(), todoRepository.count());
    }

    /**
     * 写入 6 个演示用户，覆盖不同角色与账号状态。
     */
    private List<User> initUsers() {
        List<User> users = List.of(
                User.builder()
                        .username("admin").nickname("系统管理员").email("admin@taskhub.local")
                        .phone("13800000001").department("技术管理部")
                        .role(UserRole.ADMIN).status(UserStatus.ACTIVE)
                        .remark("拥有全部权限的内置管理员账号")
                        .build(),
                User.builder()
                        .username("zhangsan").nickname("张三").email("zhangsan@taskhub.local")
                        .phone("13800000002").department("研发部")
                        .role(UserRole.MANAGER).status(UserStatus.ACTIVE)
                        .remark("负责后端迭代排期")
                        .build(),
                User.builder()
                        .username("lisi").nickname("李四").email("lisi@taskhub.local")
                        .phone("13800000003").department("研发部")
                        .role(UserRole.MEMBER).status(UserStatus.ACTIVE)
                        .remark("前端开发")
                        .build(),
                User.builder()
                        .username("wangwu").nickname("王五").email("wangwu@taskhub.local")
                        .phone("13800000004").department("设计部")
                        .role(UserRole.MEMBER).status(UserStatus.ACTIVE)
                        .remark("UI / 交互设计")
                        .build(),
                User.builder()
                        .username("zhaoliu").nickname("赵六").email("zhaoliu@taskhub.local")
                        .phone("13800000005").department("测试部")
                        .role(UserRole.MEMBER).status(UserStatus.DISABLED)
                        .remark("已离职，账号停用")
                        .build(),
                User.builder()
                        .username("guest").nickname("访客账号").email("guest@taskhub.local")
                        .department("运营部")
                        .role(UserRole.GUEST).status(UserStatus.ACTIVE)
                        .remark("只读演示账号")
                        .build()
        );

        return userRepository.saveAll(users);
    }

    /**
     * 写入 13 条演示待办，覆盖全部状态、优先级以及「已逾期 / 今日到期 / 未分配」等边界场景。
     *
     * @param users 已持久化的用户列表（带 ID）
     */
    private void initTodos(List<User> users) {
        User admin = users.get(0);
        User zhangsan = users.get(1);
        User lisi = users.get(2);
        User wangwu = users.get(3);

        LocalDate today = LocalDate.now();
        List<TodoItem> todos = new ArrayList<>();

        // ---------- 已逾期 ----------
        todos.add(TodoItem.builder()
                .title("修复登录接口偶发 500 问题").description("线上日志显示偶发空指针，需补充参数兜底与单元测试")
                .status(TodoStatus.IN_PROGRESS).priority(TodoPriority.URGENT)
                .dueDate(today.minusDays(2)).assignee(zhangsan).build());
        todos.add(TodoItem.builder()
                .title("补充 API 接口文档").description("对齐 Swagger 分组与字段说明")
                .status(TodoStatus.PENDING).priority(TodoPriority.HIGH)
                .dueDate(today.minusDays(1)).assignee(lisi).build());

        // ---------- 今日到期 ----------
        todos.add(TodoItem.builder()
                .title("评审首页仪表盘设计稿").description("重点确认统计卡片的指标口径与配色方案")
                .status(TodoStatus.PENDING).priority(TodoPriority.HIGH)
                .dueDate(today).assignee(wangwu).build());
        todos.add(TodoItem.builder()
                .title("整理本周迭代待办清单")
                .status(TodoStatus.IN_PROGRESS).priority(TodoPriority.MEDIUM)
                .dueDate(today).assignee(admin).build());

        // ---------- 进行中 ----------
        todos.add(TodoItem.builder()
                .title("实现待办事项分页查询接口").description("支持关键字、状态、优先级、负责人组合筛选")
                .status(TodoStatus.IN_PROGRESS).priority(TodoPriority.HIGH)
                .dueDate(today.plusDays(3)).assignee(zhangsan).build());
        todos.add(TodoItem.builder()
                .title("封装 Axios 请求拦截器").description("统一处理响应解包、错误提示与登录态失效跳转")
                .status(TodoStatus.IN_PROGRESS).priority(TodoPriority.MEDIUM)
                .dueDate(today.plusDays(4)).assignee(lisi).build());

        // ---------- 待处理 ----------
        todos.add(TodoItem.builder()
                .title("编写项目周报").description("汇总本周研发进度、风险与下周计划，周五下班前提交")
                .status(TodoStatus.PENDING).priority(TodoPriority.MEDIUM)
                .dueDate(today.plusDays(2)).assignee(lisi).build());
        todos.add(TodoItem.builder()
                .title("设计用户管理页面表格").description("包含筛选栏、批量操作与分页组件")
                .status(TodoStatus.PENDING).priority(TodoPriority.MEDIUM)
                .dueDate(today.plusDays(6)).assignee(wangwu).build());
        todos.add(TodoItem.builder()
                .title("接入全局异常处理器")
                .status(TodoStatus.PENDING).priority(TodoPriority.LOW)
                .dueDate(today.plusDays(9)).assignee(zhangsan).build());
        todos.add(TodoItem.builder()
                .title("规划下一版本功能范围").description("尚未确定负责人，待周会讨论后分配")
                .status(TodoStatus.PENDING).priority(TodoPriority.MEDIUM)
                .dueDate(today.plusDays(14)).build());

        // ---------- 已完成 ----------
        todos.add(TodoItem.builder()
                .title("搭建 Spring Boot 项目骨架").description("完成分层结构、统一响应体与配置多环境")
                .status(TodoStatus.DONE).priority(TodoPriority.HIGH)
                .dueDate(today.minusDays(5)).completedAt(today.minusDays(6).atTime(17, 30))
                .assignee(admin).build());
        todos.add(TodoItem.builder()
                .title("初始化 Vue 3 + TypeScript 前端工程")
                .status(TodoStatus.DONE).priority(TodoPriority.HIGH)
                .dueDate(today.minusDays(4)).completedAt(today.minusDays(4).atTime(11, 10))
                .assignee(lisi).build());

        // ---------- 已取消 ----------
        todos.add(TodoItem.builder()
                .title("引入 Redis 做接口缓存").description("当前数据量较小，评估后决定暂缓")
                .status(TodoStatus.CANCELLED).priority(TodoPriority.LOW)
                .dueDate(today.plusDays(20)).assignee(zhangsan).build());

        List<TodoItem> saved = todoRepository.saveAll(todos);
        log.debug("演示待办写入完成，共 {} 条", saved.size());
    }
}

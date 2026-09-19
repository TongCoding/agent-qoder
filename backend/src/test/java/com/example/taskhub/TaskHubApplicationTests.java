package com.example.taskhub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应用上下文加载测试。
 *
 * <p>这是成本最低的「冒烟测试」：如果 Bean 装配、JPA 实体映射、配置绑定中任何一环出错，
 * 该用例都会失败，可以在 CI 中快速拦截配置类问题。</p>
 *
 * @author TaskHub
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskHubApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Spring 容器能够正常启动")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getEnvironment().getActiveProfiles()).contains("test");
    }

    @Test
    @DisplayName("核心分层 Bean 均已注册")
    void coreBeansAreRegistered() {
        assertThat(applicationContext.getBean("todoController")).isNotNull();
        assertThat(applicationContext.getBean("userController")).isNotNull();
        assertThat(applicationContext.getBean("todoServiceImpl")).isNotNull();
        assertThat(applicationContext.getBean("userServiceImpl")).isNotNull();
        assertThat(applicationContext.getBean("globalExceptionHandler")).isNotNull();
    }
}

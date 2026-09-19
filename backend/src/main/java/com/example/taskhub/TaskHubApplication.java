package com.example.taskhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * TaskHub 后端服务启动类。
 *
 * <p>本项目为前后端分离架构中的服务端，提供「用户管理」与「待办事项」两组 REST API，
 * 使用 H2 内存数据库存储数据，并集成 springdoc-openapi 提供在线接口文档。</p>
 *
 * <ul>
 *   <li>接口文档：http://localhost:8080/swagger-ui.html</li>
 *   <li>H2 控制台：http://localhost:8080/h2-console（仅 dev 环境）</li>
 *   <li>健康检查：http://localhost:8080/actuator/health</li>
 * </ul>
 *
 * @author TaskHub
 * @since 1.0.0
 */
@SpringBootApplication
// 扫描 @ConfigurationProperties 注解的配置类（如 AppProperties）
@ConfigurationPropertiesScan
// 开启 JPA 审计，自动填充 createdAt / updatedAt 字段
@EnableJpaAuditing
public class TaskHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskHubApplication.class, args);
    }
}

package com.example.taskhub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3（Swagger）文档配置。
 *
 * <p>访问地址：</p>
 * <ul>
 *   <li>Swagger UI：http://localhost:8080/swagger-ui.html</li>
 *   <li>OpenAPI JSON：http://localhost:8080/v3/api-docs</li>
 *   <li>分组 JSON：http://localhost:8080/v3/api-docs/todo-api</li>
 * </ul>
 *
 * <p>除全局文档信息外，这里还按业务模块划分了两个分组，便于在 UI 右上角切换查看。</p>
 *
 * @author TaskHub
 */
@Configuration
public class OpenApiConfig {

    /**
     * 全局文档元信息。
     */
    @Bean
    public OpenAPI taskHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskHub API")
                        .version("1.0.0")
                        .description("""
                                TaskHub 后端接口文档。

                                本项目是前后端分离的全栈示例，提供「用户管理」与「待办事项」两组 REST API。

                                **通用约定**
                                - 所有接口统一返回 `{ code, message, data, timestamp }` 结构，`code = 0` 表示成功；
                                - 分页参数 `page` 从 **1** 开始，`size` 最大 100；
                                - 时间字段使用 ISO-8601 字符串，时区为 GMT+8；
                                - 出错时 `code` 为 5 位业务错误码，`data` 可能包含字段级校验明细。
                                """)
                        .contact(new Contact()
                                .name("TaskHub Team")
                                .email("dev@taskhub.local"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                // 显式声明服务地址，Swagger UI 的 "Try it out" 才能正确发起请求
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("本地开发环境")));
    }

    /**
     * 待办事项接口分组。
     */
    @Bean
    public GroupedOpenApi todoApi() {
        return GroupedOpenApi.builder()
                .group("todo-api")
                .displayName("01. 待办事项")
                .pathsToMatch("/api/v1/todos/**")
                .build();
    }

    /**
     * 用户管理接口分组。
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user-api")
                .displayName("02. 用户管理")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }

    /**
     * 全部接口分组（默认视图）。
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("00. 全部接口")
                .pathsToMatch("/api/**")
                .build();
    }
}

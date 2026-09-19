package com.example.taskhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 应用自定义配置，对应 {@code application.yml} 中的 {@code app.*} 节点。
 *
 * <p>使用 {@code @ConfigurationProperties} 而非散落的 {@code @Value}，
 * 好处是类型安全、支持 IDE 自动补全，并且便于集中管理。</p>
 *
 * <p>该类通过启动类上的 {@code @ConfigurationPropertiesScan} 自动注册为 Bean。</p>
 *
 * @author TaskHub
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 跨域配置 */
    private Cors cors = new Cors();

    /** 是否在启动时写入演示数据 */
    private boolean seedData = true;

    /**
     * 跨域（CORS）相关配置。
     */
    @Getter
    @Setter
    public static class Cors {

        /** 允许的来源，支持通配符模式（如 https://*.example.com） */
        private List<String> allowedOrigins = List.of("http://localhost:5173");

        /** 允许的 HTTP 方法 */
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

        /** 允许的请求头 */
        private List<String> allowedHeaders = List.of("*");

        /** 是否允许携带凭证（Cookie / Authorization） */
        private boolean allowCredentials = true;

        /** 预检请求结果的缓存时间（秒） */
        private long maxAge = 3600L;
    }
}

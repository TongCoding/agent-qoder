package com.example.taskhub.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置：目前主要负责跨域（CORS）策略。
 *
 * <p>前后端分离项目在本地开发时，前端运行于 {@code http://localhost:5173}，
 * 后端运行于 {@code http://localhost:8080}，浏览器会因同源策略拦截请求，
 * 因此需要服务端显式放行。生产环境若由 Nginx 统一域名反向代理，可将此处来源收敛为空。</p>
 *
 * <p>说明：这里使用 {@code allowedOriginPatterns} 而非 {@code allowedOrigins}，
 * 因为当 {@code allowCredentials = true} 时，Spring 不允许 {@code allowedOrigins} 使用通配符 {@code *}，
 * 而 {@code allowedOriginPatterns} 支持 {@code https://*.example.com} 这类模式匹配。</p>
 *
 * @author TaskHub
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        AppProperties.Cors cors = appProperties.getCors();

        registry.addMapping("/api/**")
                .allowedOriginPatterns(cors.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods(cors.getAllowedMethods().toArray(String[]::new))
                .allowedHeaders(cors.getAllowedHeaders().toArray(String[]::new))
                .allowCredentials(cors.isAllowCredentials())
                .maxAge(cors.getMaxAge());

        log.info("CORS 已配置 | 放行来源={} | 放行方法={}", cors.getAllowedOrigins(), cors.getAllowedMethods());
    }
}

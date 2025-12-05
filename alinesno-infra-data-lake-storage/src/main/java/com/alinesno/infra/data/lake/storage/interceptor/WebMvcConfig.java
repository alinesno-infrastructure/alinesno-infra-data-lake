package com.alinesno.infra.data.lake.storage.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final DownloadTokenInterceptor downloadTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有下载相关路径（可根据实际 controller 路径调整）
        registry.addInterceptor(downloadTokenInterceptor)
                .addPathPatterns("/api/lake/storage/download/**");
    }
}
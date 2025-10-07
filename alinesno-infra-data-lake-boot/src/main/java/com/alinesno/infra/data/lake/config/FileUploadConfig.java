package com.alinesno.infra.data.lake.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * 文件上传配置
 */
@Configuration
public class FileUploadConfig {

@Bean
public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();

        // 设置单个文件的最大大小
        factory.setMaxFileSize(DataSize.ofMegabytes(500));

        // 设置整个请求的最大大小
        factory.setMaxRequestSize(DataSize.ofMegabytes(500));

        return factory.createMultipartConfig();
    }
}
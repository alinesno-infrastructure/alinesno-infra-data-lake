package com.alinesno.infra.data.lake.config;

import com.alinesno.infra.data.lake.properties.IcebergProperties;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置
 */
@Data
@Configuration
public class AliyunOSSConfig {

    private final IcebergProperties properties;

    public AliyunOSSConfig(IcebergProperties properties) {
        this.properties = properties;
    }

    @Bean
    public OSS ossClient() {

        String endpoint = properties.getOss().getEndpoint();
        String accessKeyId = properties.getOss().getAccessKeyId();
        String accessKeySecret = properties.getOss().getAccessKeySecret();

        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}

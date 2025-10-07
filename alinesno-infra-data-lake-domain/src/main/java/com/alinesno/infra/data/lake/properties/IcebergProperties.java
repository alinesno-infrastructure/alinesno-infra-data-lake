package com.alinesno.infra.data.lake.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Iceberg属性配置类
 */
@Data
@ConfigurationProperties(prefix = "iceberg")
public class IcebergProperties {

    // 存储类型：minio或oss
    private String storageType;

    // 数据仓库路径
    private String warehousePath;

    // MinIO配置
    private MinioProperties minio = new MinioProperties();

    // 阿里云OSS配置
    private OssProperties oss = new OssProperties();

    // MinIO配置内部类
    @Data
    public static class MinioProperties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
    }

    // 阿里云OSS配置内部类
    @Data
    public static class OssProperties {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
    }
}

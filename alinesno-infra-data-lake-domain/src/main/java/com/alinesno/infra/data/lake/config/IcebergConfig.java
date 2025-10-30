package com.alinesno.infra.data.lake.config;

import com.alinesno.infra.data.lake.constants.LakeConstants;
import com.alinesno.infra.data.lake.properties.IcebergProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.jdbc.JdbcCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Iceberg 配置类（专门为 MinIO 配置 s3a）
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(IcebergProperties.class)
public class IcebergConfig {

    private final IcebergProperties properties;

    @Value("${spring.datasource.driver-class-name}")
    private String jdbcDriver;

    @Value("${spring.datasource.url}")
    private String jdbcUrl ;

    @Value("${spring.datasource.username}")
    private String jdbcUsername ;

    @Value("${spring.datasource.password}")
    private String jdbcPassword;

    public IcebergConfig(IcebergProperties properties) {
        this.properties = properties;
    }

    /**
     * 构建基于 JDBC 的 Iceberg Catalog（并针对 MinIO 配置 s3a）
     */
    @SneakyThrows
    @Bean(name = "jdbcIcebergCatalog")
    public Catalog icebergCatalog() {

        // Hadoop 配置（用于 s3a）
        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();

        // 确保 JDBC driver 在运行时类路径中
        Class.forName(jdbcDriver);

        // JDBC Catalog 属性
        Map<String, String> jdbcProperties = new HashMap<>();
        jdbcProperties.put(CatalogProperties.CATALOG_IMPL, JdbcCatalog.class.getName());
        jdbcProperties.put(CatalogProperties.URI, jdbcUrl);
        jdbcProperties.put(JdbcCatalog.PROPERTY_PREFIX + "user", jdbcUsername);
        jdbcProperties.put(JdbcCatalog.PROPERTY_PREFIX + "password", jdbcPassword);
        jdbcProperties.put(CatalogProperties.WAREHOUSE_LOCATION, properties.getWarehousePath());

        // 全局 s3a 默认项（确保使用 S3AFileSystem）
        hadoopConf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");

        // 一些默认的性能/超时设置（可根据需要调整）
        hadoopConf.setInt("fs.s3a.threads.max", 50);
        hadoopConf.set("fs.s3a.connection.maximum", "100");
        hadoopConf.set("fs.s3a.connection.timeout", "5000");
        hadoopConf.set("fs.s3a.connection.establish.timeout", "5000");
        hadoopConf.set("fs.s3a.attempts.maximum", "3");
        hadoopConf.set("fs.s3a.retry.limit", "3");

        // 仅支持 MinIO 存储（s3a）
        if ("minio".equalsIgnoreCase(properties.getStorageType())) {
            configureMinIO(hadoopConf);
        } else if ("aliyun-oss".equalsIgnoreCase(properties.getStorageType())) {
            // 保留阿里 OSS 配置方法（如果你仍需要）
            configureAliyunOSS(hadoopConf);
        } else {
            // 如果你只需要 MinIO，可抛出异常或记录警告
            log.warn("storageType is not recognized as 'minio' or 'aliyun-oss', current: {}", properties.getStorageType());
        }

        // 可选：快速连通性检查（非必须），仅用于早期发现 s3a/MinIO 访问问题
        try {
            validateS3aAccess(hadoopConf, properties.getWarehousePath());
        } catch (Exception e) {
            log.warn("MinIO connectivity quick check failed: {}", e.getMessage());
            // 不阻止 Catalog 创建，继续创建以便 Iceberg 自身处理失败场景
        }

        // 构建并返回 Iceberg Catalog
        return CatalogUtil.buildIcebergCatalog(LakeConstants.DEFAULT_CATALOG_NAME, jdbcProperties, hadoopConf);
    }

    /**
     * 配置 MinIO（使用 s3a 指向 MinIO endpoint）
     */
    private void configureMinIO(org.apache.hadoop.conf.Configuration conf) {
        // endpoint 建议为 host:port（例如 minio-host:9000）
        String endpoint = properties.getMinio().getEndpoint();
        boolean ssl = properties.getMinio().isSslEnabled(); // 假设 IcebergProperties 中有此字段（若无请自行添加）
        String accessKey = properties.getMinio().getAccessKey();
        String secretKey = properties.getMinio().getSecretKey();

        // 基本 s3a -> MinIO 配置
        conf.set("fs.s3a.endpoint", endpoint);
        conf.setBoolean("fs.s3a.path.style.access", true); // MinIO 需要 path-style
        conf.setBoolean("fs.s3a.connection.ssl.enabled", ssl);

        // 认证
        conf.set("fs.s3a.access.key", accessKey);
        conf.set("fs.s3a.secret.key", secretKey);

        // 使用简单凭证提供器，避免 AWS InstanceProfile 等
        conf.set("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");

        // 若 endpoint 中带协议（不推荐），s3a 在不同 Hadoop 版本上的表现不同。推荐使用 host:port + ssl flag。
        log.info("Configured MinIO s3a endpoint={}, sslEnabled={}", endpoint, ssl);
    }

    /**
     * 配置阿里云 OSS（保留，用于可选场景）
     */
    private void configureAliyunOSS(org.apache.hadoop.conf.Configuration conf) {
        conf.set("fs.oss.impl", "org.apache.hadoop.fs.aliyun.oss.AliyunOSSFileSystem");
        conf.set("fs.AbstractFileSystem.oss.impl", "org.apache.hadoop.fs.aliyun.oss.AliyunOSS");
        conf.set("fs.oss.endpoint", properties.getOss().getEndpoint());
        conf.set("fs.oss.accessKeyId", properties.getOss().getAccessKeyId());
        conf.set("fs.oss.accessKeySecret", properties.getOss().getAccessKeySecret());
    }

    /**
     * 可选的快速连通性检查：尝试使用 s3a filesystem 列出 warehouse 根路径
     * 如果你不想在启动时做此检查，可以移除或注释此方法的调用
     */
    private void validateS3aAccess(org.apache.hadoop.conf.Configuration conf, String warehousePath) throws Exception {
        if (warehousePath == null || !warehousePath.startsWith("s3a://")) {
            log.info("warehousePath is not s3a://..., skip s3a quick check: {}", warehousePath);
            return;
        }

        try {
            // 尝试从 s3a URI 获取 FileSystem 并列出根路径，快速发现认证/连通问题
            URI uri = new URI(warehousePath);
            org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(uri, conf);
            org.apache.hadoop.fs.Path root = new org.apache.hadoop.fs.Path("/");
            org.apache.hadoop.fs.FileStatus[] statuses = fs.listStatus(root);
            log.info("s3a quick check succeeded, found {} items at root of bucket", statuses == null ? 0 : statuses.length);
        } catch (Throwable t) {
            log.warn("s3a quick check failed for {}: {}", warehousePath, t.getMessage());
            throw new Exception("s3a quick check failed", t);
        }
    }
}
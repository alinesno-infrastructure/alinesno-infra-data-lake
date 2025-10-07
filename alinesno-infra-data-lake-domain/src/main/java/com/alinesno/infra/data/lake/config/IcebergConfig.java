package com.alinesno.infra.data.lake.config;

import com.alinesno.infra.data.lake.constants.LakeConstants;
import com.alinesno.infra.data.lake.properties.IcebergProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.jdbc.JdbcCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Iceberg 配置类
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

    @SneakyThrows
    @Bean(name = "jdbcIcebergCatalog")
    public JdbcCatalog icebergCatalog() {

        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration(); // configs if you use HadoopFileIO
        Class.forName(jdbcDriver); // ensure JDBC driver is at runtime classpath

        Map<String, String> jdbcProperties = new HashMap<>();
        jdbcProperties.put(CatalogProperties.CATALOG_IMPL, JdbcCatalog.class.getName());
        jdbcProperties.put(CatalogProperties.URI, jdbcUrl) ;
        jdbcProperties.put(JdbcCatalog.PROPERTY_PREFIX + "user", jdbcUsername) ;
        jdbcProperties.put(JdbcCatalog.PROPERTY_PREFIX + "password", jdbcPassword) ;
        jdbcProperties.put(CatalogProperties.WAREHOUSE_LOCATION, properties.getWarehousePath()) ;

        // 通用S3配置
        hadoopConf.set("fs.s3a.aws.credentials.provider",
                "com.amazonaws.auth.InstanceProfileCredentialsProvider," +
                        "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider," +
                        "com.amazonaws.auth.EnvironmentVariableCredentialsProvider");

        hadoopConf.set("fs.s3a.connection.ssl.enabled", "false");
        hadoopConf.set("fs.s3a.path.style.access", "true");
        hadoopConf.set("fs.s3a.fast.upload", "true");

        // 根据配置类型设置不同的存储配置
        if ("minio".equals(properties.getStorageType())) {
            configureMinIO(hadoopConf);
        } else if ("aliyun-oss".equals(properties.getStorageType())) {
            configureAliyunOSS(hadoopConf);
        }

        return (JdbcCatalog) CatalogUtil.buildIcebergCatalog(LakeConstants.DEFAULT_CATALOG_NAME, jdbcProperties, hadoopConf);
    }

    /**
     * 配置MinIO
     * @param conf
     */
    private void configureMinIO(org.apache.hadoop.conf.Configuration conf) {
        conf.set("fs.s3a.endpoint", properties.getMinio().getEndpoint());
        conf.set("fs.s3a.access.key", properties.getMinio().getAccessKey());
        conf.set("fs.s3a.secret.key", properties.getMinio().getSecretKey());
        conf.set("fs.oss.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
    }

    /**
     * 配置阿里云OSS
     * @param conf
     */
    private void configureAliyunOSS(org.apache.hadoop.conf.Configuration conf) {
        conf.set("fs.oss.impl", "org.apache.hadoop.fs.aliyun.oss.AliyunOSSFileSystem");
        conf.set("fs.AbstractFileSystem.oss.impl", "org.apache.hadoop.fs.aliyun.oss.AliyunOSS");
        conf.set("fs.oss.endpoint", properties.getOss().getEndpoint());
        conf.set("fs.oss.accessKeyId", properties.getOss().getAccessKeyId());
        conf.set("fs.oss.accessKeySecret", properties.getOss().getAccessKeySecret());
    }

}

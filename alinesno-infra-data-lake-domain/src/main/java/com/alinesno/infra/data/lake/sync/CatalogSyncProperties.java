package com.alinesno.infra.data.lake.sync;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

/**
 * CatalogSyncProperties
 */
@Setter
@Getter
@EnableScheduling
@Component
@ConfigurationProperties(prefix = "data.lake.sync")
public class CatalogSyncProperties {

    // 每页读取多少个 distinct catalogId
    private int pageSize = 100;
    // 并发线程池大小
    private int threadPoolSize = 5;

    // 单个 catalog 同步最大等待时间（毫秒），到达后记录并继续
    private long perTaskTimeoutMillis = 10 * 60 * 1000L; // 10 分钟

}
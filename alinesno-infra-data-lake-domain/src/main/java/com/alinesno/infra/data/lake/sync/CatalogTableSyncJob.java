package com.alinesno.infra.data.lake.sync;

import com.alinesno.infra.data.lake.entity.CatalogTableEntity;
import com.alinesno.infra.data.lake.mapper.CatalogTableMapper;
import com.alinesno.infra.data.lake.service.ICatalogTableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CatalogTableSyncJob
 */
@Slf4j
@Component
public class CatalogTableSyncJob {

    @Autowired
    private CatalogSyncProperties properties;

    @Autowired
    private CatalogTableMapper catalogTableMapper;

    @Autowired
    private ICatalogTableService catalogTableService;

    // 受控线程池，用于并发同步多个 catalog
    private ExecutorService executor;

    // 防止任务重叠执行（单实例）
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        int threads = Math.max(1, properties.getThreadPoolSize());
        this.executor = new ThreadPoolExecutor(
                threads, threads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(threads * 10),
                new ThreadFactory() {
                    private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                    private final String prefix = "catalog-sync-";
                    private final AtomicInteger seq = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = defaultFactory.newThread(r);
                        t.setName(prefix + seq.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 每 5 分钟执行一次。cron 可替换为 fixedDelay/fixedRate 等。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void scheduledSync() {
        if (!running.compareAndSet(false, true)) {
            log.info("CatalogTableSyncJob: 上一次任务仍在运行，跳过本次调度");
            return;
        }

        long start = System.currentTimeMillis();
        log.info("CatalogTableSyncJob: 开始执行自动同步任务");

        try {
            final int pageSize = Math.max(1, properties.getPageSize());

            int offset = 0;
            List<Future<?>> allFutures = new ArrayList<>();

            while (true) {
                // 通过 MyBatis-Plus wrapper 获取 distinct catalogId，使用 last() 限制分页
                LambdaQueryWrapper<CatalogTableEntity> wrapper = new LambdaQueryWrapper<>();
                wrapper.select(CatalogTableEntity::getCatalogId);
                wrapper.groupBy(CatalogTableEntity::getCatalogId);
                wrapper.last("LIMIT " + pageSize + " OFFSET " + offset);

                List<Object> rows = catalogTableMapper.selectObjs(wrapper);

                if (rows == null || rows.isEmpty()) {
                    break;
                }

                for (Object row : rows) {
                    final long catalogId;
                    if (row instanceof Number) {
                        catalogId = ((Number) row).longValue();
                    } else {
                        // 有时 selectObjs 返回 Map 或其它，做安全转换
                        catalogId = Long.parseLong(String.valueOf(row));
                    }

                    // 提交任务，单个 catalog 的异常不会影响其他任务
                    Future<?> future = executor.submit(() -> {
                        try {
                            log.info("开始同步 catalogId={}", catalogId);
                            // 这里只调用 service 的方法（方法内部负责具体同步与分页处理）
                            catalogTableService.syncTableStructure(catalogId);
                            log.info("完成同步 catalogId={}", catalogId);
                        } catch (Exception ex) {
                            log.error("同步 catalogId={} 失败", catalogId, ex);
                            // 这里可以考虑重试策略或告警
                        }
                    });

                    allFutures.add(future);
                }

                if (rows.size() < pageSize) {
                    break; // 最后一页
                }
                offset += pageSize;
            }

            // 等待所有任务完成，但对单个任务做超时保护，避免某一异常任务长期挂住
            long perTaskTimeout = Math.max(1000L, properties.getPerTaskTimeoutMillis());
            for (Future<?> f : allFutures) {
                try {
                    f.get(perTaskTimeout, TimeUnit.MILLISECONDS);
                } catch (TimeoutException te) {
                    log.warn("某个 catalog 同步任务超时（{} ms），将继续执行其他任务", perTaskTimeout);
                    // 不取消任务（也可以选择取消）
                } catch (ExecutionException ee) {
                    log.error("某个 catalog 同步任务执行失败", ee.getCause());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("等待任务时线程被中断");
                }
            }

            long cost = System.currentTimeMillis() - start;
            log.info("CatalogTableSyncJob: 自动同步任务完成, 耗时 {} ms", cost);

        } catch (Exception e) {
            log.error("CatalogTableSyncJob: 执行异常", e);
        } finally {
            running.set(false);
        }
    }
}
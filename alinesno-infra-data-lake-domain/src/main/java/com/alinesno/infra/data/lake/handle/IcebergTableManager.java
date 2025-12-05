package com.alinesno.infra.data.lake.handle;

import com.alinesno.infra.data.lake.api.TableStatisticsDto;
import com.alinesno.infra.data.lake.api.iceberg.DatabaseStorageStats;
import com.alinesno.infra.data.lake.api.iceberg.TableInfoDto;
import com.alinesno.infra.data.lake.api.iceberg.TableStorageStats;
import com.alinesno.infra.data.lake.properties.IcebergProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.*;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.types.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Iceberg表管理工具类
 */
@Slf4j
@Component
public class IcebergTableManager {

    @Resource(name = "jdbcIcebergCatalog")
    private Catalog icebergCatalog;

    @Autowired
    private IcebergProperties icebergProperties;

    /**
     * 创建Iceberg表
     * @param databaseName 数据库名
     * @param tableName 表名
     * @param schema 表schema
     * @param partitionSpec 分区规则
     * @param properties 表属性
     * @return 创建的Table对象
     */
    public Table createTable(String databaseName, String tableName, Schema schema,
                             PartitionSpec partitionSpec, Map<String, String> properties) {

        try {
            Namespace namespace = Namespace.of(databaseName);
            TableIdentifier tableIdentifier = TableIdentifier.of(namespace, tableName);

            // 检查表是否已存在
            if (icebergCatalog.tableExists(tableIdentifier)) {
                throw new RuntimeException("表已存在: " + databaseName + "." + tableName);
            }

            // 设置默认属性
            Map<String, String> tableProperties = new HashMap<>();
            tableProperties.put("format-version", "2");
            tableProperties.put("write.parquet.compression-codec", "zstd");

            if (properties != null) {
                tableProperties.putAll(properties);
            }

            // 创建表
            Table table = icebergCatalog.createTable(
                    tableIdentifier,
                    schema,
                    partitionSpec != null ? partitionSpec : PartitionSpec.unpartitioned(),
                    tableProperties
            );

            log.info("Iceberg表创建成功: {}.{}", databaseName, tableName);
            return table;

        } catch (Exception e) {
            log.error("创建Iceberg表失败: {}.{}", databaseName, tableName, e);
            throw new RuntimeException("创建Iceberg表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除Iceberg表
     * @param databaseName 数据库名
     * @param tableName 表名
     * @param purge 是否彻底删除数据文件
     */
    public void dropTable(String databaseName, String tableName, boolean purge) {
        try {
            TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);

            if (icebergCatalog.tableExists(tableIdentifier)) {
                if (purge) {
                    icebergCatalog.dropTable(tableIdentifier, true);
                } else {
                    icebergCatalog.dropTable(tableIdentifier);
                }
                log.info("Iceberg表删除成功: {}.{}", databaseName, tableName);
            } else {
                log.warn("Iceberg表不存在: {}.{}", databaseName, tableName);
            }
        } catch (Exception e) {
            log.error("删除Iceberg表失败: {}.{}", databaseName, tableName, e);
            throw new RuntimeException("删除Iceberg表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查表是否存在
     * @param databaseName 数据库名
     * @param tableName 表名
     * @return 是否存在
     */
    public boolean tableExists(String databaseName, String tableName) {
        try {
            TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);
            return icebergCatalog.tableExists(tableIdentifier);
        } catch (Exception e) {
            log.error("检查Iceberg表存在失败: {}.{}", databaseName, tableName, e);
            return false;
        }
    }

    /**
     * 获取表存储位置
     * @param databaseName 数据库名
     * @param tableName 表名
     * @return 表存储路径
     */
    public String getTableLocation(String databaseName, String tableName) {
        try {
            TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);
            if (icebergCatalog.tableExists(tableIdentifier)) {
                Table table = icebergCatalog.loadTable(tableIdentifier);
                return table.location();
            }
            return null;
        } catch (Exception e) {
            log.error("获取表存储位置失败: {}.{}", databaseName, tableName, e);
            return null;
        }
    }

    /**
     * 构建默认的Schema（可根据实际需求扩展）
     */
    public Schema createDefaultSchema() {
        return new Schema(
                Types.NestedField.required(1, "id", Types.LongType.get()),
                Types.NestedField.optional(2, "data", Types.StringType.get()),
                Types.NestedField.optional(3, "create_time", Types.TimestampType.withZone()),
                Types.NestedField.optional(4, "update_time", Types.TimestampType.withZone())
        );
    }

//    /**
//     * 构建表存储路径
//     * @param catalogId 目录ID
//     * @param tableName 表名
//     * @return 完整的表存储路径
//     */
//    public String buildTableStoragePath(Long catalogId, String tableName) {
//        return String.format("%s/%s/%s",
//                icebergProperties.getWarehousePath(),
//                "catalog_" + catalogId,
//                tableName);
//    }

    /**
     * 加载Iceberg表
     * @param databaseName 数据库名称
     * @param tableName 表名称
     * @return Iceberg表对象，如果表不存在则返回null
     */
    public Table loadTable(String databaseName, String tableName) {
        try {
            // 构建表标识符
            TableIdentifier tableIdentifier = TableIdentifier.of(databaseName, tableName);

            // 检查表是否存在
            if (!icebergCatalog.tableExists(tableIdentifier)) {
                return null;
            }

            // 加载表
            return icebergCatalog.loadTable(tableIdentifier);

        } catch (Exception e) {
            throw new RuntimeException("加载Iceberg表失败: " + databaseName + "." + tableName, e);
        }
    }

    /**
     * 检查数据库是否存在
     * @param databaseName 数据库名称
     * @return 是否存在
     */
    public boolean databaseExists(String databaseName) {
        try {
            if (icebergCatalog != null) {
                org.apache.iceberg.catalog.SupportsNamespaces namespaceCatalog =
                        (org.apache.iceberg.catalog.SupportsNamespaces) icebergCatalog;

                Namespace namespace = Namespace.of(databaseName);
                return namespaceCatalog.namespaceExists(namespace);
            }
            return false;
        } catch (Exception e) {
            log.error("检查数据库存在失败: {}", databaseName, e);
            return false;
        }
    }


    /**
     * 创建数据库/命名空间
     * @param databaseName 数据库名称
     * @param properties 数据库属性
     */
    public void createDatabase(String databaseName, Map<String, String> properties) {
        try {
            Namespace namespace = Namespace.of(databaseName);

            // 检查数据库是否已存在
            if (databaseExists(databaseName)) {
                log.warn("数据库已存在: {}", databaseName);
                return;
            }

            // 创建数据库
            if (icebergCatalog != null) {
                org.apache.iceberg.catalog.SupportsNamespaces namespaceCatalog =
                        (org.apache.iceberg.catalog.SupportsNamespaces) icebergCatalog;

                namespaceCatalog.createNamespace(namespace, properties != null ? properties : new HashMap<>());
                log.info("数据库创建成功: {}", databaseName);
            } else {
                log.warn("当前Catalog不支持命名空间操作: {}", icebergCatalog.getClass().getSimpleName());
                throw new RuntimeException("当前Catalog不支持创建数据库");
            }
        } catch (Exception e) {
            log.error("创建数据库失败: {}", databaseName, e);
            throw new RuntimeException("创建数据库失败: " + e.getMessage(), e);
        }
    }


    /**
     * 删除数据库
     * @param databaseName 数据库名称
     * @param cascade 是否级联删除表
     */
    public void dropDatabase(String databaseName, boolean cascade) {
        try {
            if (!databaseExists(databaseName)) {
                log.warn("数据库不存在: {}", databaseName);
                return;
            }

            if (icebergCatalog != null) {
                org.apache.iceberg.catalog.SupportsNamespaces namespaceCatalog =
                        (org.apache.iceberg.catalog.SupportsNamespaces) icebergCatalog;

                Namespace namespace = Namespace.of(databaseName);

                // 如果级联删除，先删除所有表
                if (cascade) {
                    List<TableIdentifier> tables = icebergCatalog.listTables(namespace);
                    for (TableIdentifier tableId : tables) {
                        dropTable(databaseName, tableId.name(), true);
                    }
                }

                namespaceCatalog.dropNamespace(namespace);
                log.info("数据库删除成功: {}", databaseName);
            } else {
                log.warn("当前Catalog不支持命名空间操作");
                throw new RuntimeException("当前Catalog不支持删除数据库");
            }
        } catch (Exception e) {
            log.error("删除数据库失败: {}", databaseName, e);
            throw new RuntimeException("删除数据库失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取数据库列表
     * @return 数据库名称列表
     */
    public List<String> listDatabases() {
        try {
            if (icebergCatalog instanceof org.apache.iceberg.catalog.SupportsNamespaces namespaceCatalog) {

            List<Namespace> namespaces = namespaceCatalog.listNamespaces();
            return namespaces.stream()
                    .map(ns -> ns.level(0)) // 获取第一级命名空间（数据库名）
                    .toList();
            }
            return List.of();
        } catch (Exception e) {
            log.error("获取数据库列表失败", e);
            return List.of();
        }
    }

    /**
     * 获取表的存储量统计
     * @param databaseName 数据库名称
     * @param tableName 表名称
     * @return 存储量信息（字节数）
     */
    public long getTableStorageSize(String databaseName, String tableName) {
        try {
            Table table = loadTable(databaseName, tableName);
            if (table == null) {
                return 0L;
            }

            // 获取表的所有数据文件
            long totalSize = 0L;
            for (FileScanTask task : table.newScan().planFiles()) {
                totalSize += task.file().fileSizeInBytes();
            }

            return totalSize;
        } catch (Exception e) {
            log.error("获取表存储量失败: {}.{}", databaseName, tableName, e);
            return 0L;
        }
    }

    /**
     * 获取数据库的总存储量
     * @param databaseName 数据库名称
     * @return 存储量信息（字节数）
     */
    public long getDatabaseStorageSize(String databaseName) {
        try {
            if (!databaseExists(databaseName)) {
                return 0L;
            }

            long totalSize = 0L;
            Namespace namespace = Namespace.of(databaseName);
            List<TableIdentifier> tables = icebergCatalog.listTables(namespace);

            for (TableIdentifier tableId : tables) {
                totalSize += getTableStorageSize(databaseName, tableId.name());
            }

            return totalSize;
        } catch (Exception e) {
            log.error("获取数据库存储量失败: {}", databaseName, e);
            return 0L;
        }
    }

    /**
     * 获取整个Catalog的总存储量
     * @return 存储量信息（字节数）
     */
    public long getCatalogStorageSize() {
        try {
            long totalSize = 0L;
            List<String> databases = listDatabases();

            for (String database : databases) {
                totalSize += getDatabaseStorageSize(database);
            }

            return totalSize;
        } catch (Exception e) {
            log.error("获取Catalog存储量失败", e);
            return 0L;
        }
    }

    /**
     * 获取表的详细存储统计信息
     * @param databaseName 数据库名称
     * @param tableName 表名称
     * @return 存储统计信息
     */
    public TableStorageStats getTableStorageStats(String databaseName, String tableName) {
        try {
            Table table = loadTable(databaseName, tableName);
            if (table == null) {
                return new TableStorageStats(databaseName, tableName, 0L, 0, 0L);
            }

            long totalSize = 0L;
            int fileCount = 0;
            long snapshotSize = 0L;

            // 统计数据文件
            for (FileScanTask task : table.newScan().planFiles()) {
                totalSize += task.file().fileSizeInBytes();
                fileCount++;
            }

            return new TableStorageStats(databaseName, tableName, totalSize, fileCount, snapshotSize);
        } catch (Exception e) {
            log.error("获取表存储统计失败: {}.{}", databaseName, tableName, e);
            return new TableStorageStats(databaseName, tableName, 0L, 0, 0L);
        }
    }

    /**
     * 获取表的详细统计信息
     * @param databaseName 数据库名称
     * @param tableName 表名称
     * @return TableStatisticsDto 统计信息
     */
    public TableStatisticsDto getTableStatistics(String databaseName, String tableName) {
        TableStatisticsDto dto = new TableStatisticsDto();

        try {
            Table table = loadTable(databaseName, tableName);
            if (table == null) {
                return dto;
            }

            // 基础信息
            dto.setLocation(table.location());
            dto.setTableType("ICEBERG");
            dto.setFormatType("PARQUET"); // Iceberg默认使用Parquet格式

            // 获取文件统计
            long totalSize = 0L;
            long fileCount = 0L;
            for (FileScanTask task : table.newScan().planFiles()) {
                totalSize += task.file().fileSizeInBytes();
                fileCount++;
            }
            dto.setTotalSize(totalSize);
            dto.setFileCount(fileCount);

            // 获取记录数（需要扫描数据，可能比较耗时）
            try {
                long recordCount = getRecordCount(table);
                dto.setRecordCount(recordCount);
            } catch (Exception e) {
                log.warn("获取记录数失败: {}.{}", databaseName, tableName, e);
                dto.setRecordCount(null);
            }

            // 时间信息
            Snapshot currentSnapshot = table.currentSnapshot();
            if (currentSnapshot != null) {
                dto.setCurrentSnapshotId(String.valueOf(currentSnapshot.snapshotId()));
                dto.setLastUpdated(String.valueOf(currentSnapshot.timestampMillis()));
            }

            // 表属性
            dto.setFormatVersion(table.properties().getOrDefault("format-version", "2"));
            dto.setOwner(table.properties().getOrDefault("owner", "unknown"));
            dto.setCreatedTime(getTableCreationTime(table));

            // 分区信息
            dto.setPartitioning(formatPartitionSpec(table.spec()));

            // 其他Iceberg属性
            Map<String, String> properties = new HashMap<>(table.properties());
            dto.setProperties(properties);

            // 设置特定属性
            dto.setMaxCommitRetry(properties.getOrDefault("commit.retry.num-retries", "3"));
            dto.setSortOrder(properties.getOrDefault("read.split.target-size", "128MB"));

        } catch (Exception e) {
            log.error("获取表统计信息失败: {}.{}", databaseName, tableName, e);
        }

        return dto;
    }

    /**
     * 获取表的记录数（近似值）
     */
    private long getRecordCount(Table table) {
        try {
            // 使用Iceberg的metrics来获取记录数（更高效）
            long totalRecords = 0L;
            for (FileScanTask task : table.newScan().planFiles()) {
                DataFile file = task.file();
                if (file.recordCount() > 0) {
                    totalRecords += file.recordCount();
                }
            }
            return totalRecords;
        } catch (Exception e) {
            log.warn("无法通过metrics获取记录数，尝试估算", e);
            // 备用方案：基于文件大小估算
            return estimateRecordCount(table);
        }
    }

    /**
     * 估算记录数（基于平均记录大小）
     */
    private long estimateRecordCount(Table table) {
        try {
            long totalSize = 0L;
            long estimatedCount = 0L;

            for (FileScanTask task : table.newScan().planFiles()) {
                totalSize += task.file().fileSizeInBytes();
            }

            // 假设平均每条记录约1KB
            if (totalSize > 0) {
                estimatedCount = totalSize / 1024;
            }

            return estimatedCount;
        } catch (Exception e) {
            log.warn("记录数估算失败", e);
            return 0L;
        }
    }

    /**
     * 获取表创建时间
     */
    private String getTableCreationTime(Table table) {
        try {
            // 获取最早的快照时间作为创建时间
            Iterable<Snapshot> snapshots = table.snapshots();
            long earliestTime = Long.MAX_VALUE;

            for (Snapshot snapshot : snapshots) {
                if (snapshot.timestampMillis() < earliestTime) {
                    earliestTime = snapshot.timestampMillis();
                }
            }

            if (earliestTime != Long.MAX_VALUE) {
                return String.valueOf(earliestTime);
            }
        } catch (Exception e) {
            log.warn("获取表创建时间失败", e);
        }
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * 格式化分区信息
     */
    private String formatPartitionSpec(PartitionSpec spec) {
        if (spec.isUnpartitioned()) {
            return "UNPARTITIONED";
        }

        StringBuilder sb = new StringBuilder();
        for (PartitionField field : spec.fields()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(field.name()).append("(").append(field.transform().toString()).append(")");
        }
        return sb.toString();
    }

    /**
     * 获取数据库存储统计
     */
    public DatabaseStorageStats getDatabaseStorageStats(String databaseName) {
        DatabaseStorageStats stats = new DatabaseStorageStats();
        stats.setDatabaseName(databaseName);

        try {
            if (!databaseExists(databaseName)) {
                return stats;
            }

            Namespace namespace = Namespace.of(databaseName);
            List<TableIdentifier> tables = icebergCatalog.listTables(namespace);

            long totalSize = 0L;
            long totalFiles = 0L;
            long totalRecords = 0L;

            for (TableIdentifier tableId : tables) {
                TableStatisticsDto tableStats = getTableStatistics(databaseName, tableId.name());
                if (tableStats.getTotalSize() != null) {
                    totalSize += tableStats.getTotalSize();
                }
                if (tableStats.getFileCount() != null) {
                    totalFiles += tableStats.getFileCount();
                }
                if (tableStats.getRecordCount() != null) {
                    totalRecords += tableStats.getRecordCount();
                }
            }

            stats.setTotalSize(totalSize);
            stats.setFileCount(totalFiles);
            stats.setRecordCount(totalRecords);
            stats.setTableCount(tables.size());

        } catch (Exception e) {
            log.error("获取数据库存储统计失败: {}", databaseName, e);
        }

        return stats;
    }

    /**
     * 列出数据库中的所有表
     * @param databaseName 数据库名称
     * @return 表名称列表
     */
    public List<String> listTables(String databaseName) {
        try {
            if (!databaseExists(databaseName)) {
                log.warn("数据库不存在: {}", databaseName);
                return Collections.emptyList();
            }

            Namespace namespace = Namespace.of(databaseName);
            List<TableIdentifier> tableIdentifiers = icebergCatalog.listTables(namespace);

            return tableIdentifiers.stream()
                    .map(TableIdentifier::name)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("列出数据库表失败: {}", databaseName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 列出数据库中的所有表（带详细信息）
     * @param databaseName 数据库名称
     * @return 表信息列表
     */
    public List<TableInfoDto> listTablesWithInfo(String databaseName) {
        List<TableInfoDto> tableInfos = new ArrayList<>();

        try {
            if (!databaseExists(databaseName)) {
                return tableInfos;
            }

            Namespace namespace = Namespace.of(databaseName);
            List<TableIdentifier> tableIdentifiers = icebergCatalog.listTables(namespace);

            for (TableIdentifier tableId : tableIdentifiers) {
                TableInfoDto tableInfo = new TableInfoDto();
                tableInfo.setTableName(tableId.name());
                tableInfo.setDatabaseName(databaseName);

                try {
                    Table table = loadTable(databaseName, tableId.name());
                    if (table != null) {
                        tableInfo.setLocation(table.location());
                        tableInfo.setPartitioned(!table.spec().isUnpartitioned());

                        // 获取表大小
                        long size = 0L;
                        for (FileScanTask task : table.newScan().planFiles()) {
                            size += task.file().fileSizeInBytes();
                        }
                        tableInfo.setSizeBytes(size);

                        // 获取快照信息
                        Snapshot snapshot = table.currentSnapshot();
                        if (snapshot != null) {
                            tableInfo.setLastUpdated(snapshot.timestampMillis());
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取表 {} 信息失败", tableId.name(), e);
                }

                tableInfos.add(tableInfo);
            }

        } catch (Exception e) {
            log.error("列出数据库表详细信息失败: {}", databaseName, e);
        }

        return tableInfos;
    }

    /**
     * 获取数据库中的表数量
     * @param databaseName 数据库名称
     * @return 表数量
     */
    public int getTableCount(String databaseName) {
        try {
            if (!databaseExists(databaseName)) {
                return 0;
            }

            Namespace namespace = Namespace.of(databaseName);
            List<TableIdentifier> tables = icebergCatalog.listTables(namespace);
            return tables.size();

        } catch (Exception e) {
            log.error("获取表数量失败: {}", databaseName, e);
            return 0;
        }
    }

}
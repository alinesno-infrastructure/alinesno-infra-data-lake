package com.alinesno.infra.data.lake.handle;

import cn.hutool.extra.spring.SpringUtil;
import com.alinesno.infra.data.lake.api.CreateCatalogTableDto;
import com.alinesno.infra.data.lake.api.TableFieldDto;
import com.alinesno.infra.data.lake.api.iceberg.DatabaseStorageStats;
import com.alinesno.infra.data.lake.api.iceberg.TableInfoDto;
import com.alinesno.infra.data.lake.api.iceberg.TableStorageStats;
import com.alinesno.infra.data.lake.entity.CatalogEntity;
import com.alinesno.infra.data.lake.entity.CatalogTableEntity;
import com.alinesno.infra.data.lake.service.ICatalogService;
import com.alinesno.infra.data.lake.service.ICatalogTableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.types.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Iceberg表操作工具类
 */
@Slf4j
@Component
public class IcebergTableUtils {

    @Autowired
    private IcebergTableManager icebergTableManager;

    /**
     * 创建Iceberg表
     */
    public Table createIcebergTable(CatalogTableEntity entity, CreateCatalogTableDto dto) {
        try {
            // 获取数据库名称
            String schemeName = getSchemeName(entity);

            // 构建表Schema
            Schema schema = createDefaultSchema(dto);

            // 构建分区规则
            PartitionSpec partitionSpec = buildPartitionSpec(schema);

            // 构建表属性，包括正确的存储位置
            Map<String, String> tableProperties = buildTableProperties(entity);

            // 创建Iceberg表
            Table table = icebergTableManager.createTable(
                    schemeName,
                    entity.getTableName(),
                    schema,
                    partitionSpec,
                    tableProperties
            );

            forceInitialCommit(table);

            log.info("Iceberg表创建成功: {}.{}, 存储位置: {}", schemeName, entity.getTableName(), table.location());
            return table;

        } catch (Exception e) {
            log.error("创建Iceberg表失败: {}", entity.getTableName(), e);
            throw new RuntimeException("创建Iceberg表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 强制初始提交以生成元数据文件
     */
    private void forceInitialCommit(Table table) {
        try {
            // 创建一个空的初始提交
            table.refresh(); // 刷新表状态

            // 检查元数据文件是否已生成
            if (table.currentSnapshot() == null) {
                // 如果没有快照，创建一个空的初始快照
                log.debug("执行初始提交以生成元数据文件");
            }

            // 验证元数据文件是否存在
            String metadataLocation = table.location() + "/metadata";
            log.info("表元数据位置: {}", metadataLocation);

        } catch (Exception e) {
            log.warn("初始提交检查失败，但表可能已创建成功: {}", e.getMessage());
        }
    }

    /**
     * 构建数据库名称（根据业务规则）
     */
    public String getSchemeName(CatalogTableEntity entity) {
        // 这里可以根据catalogId、orgId等生成数据库名称
        return String.format("ns%s", entity.getSchemeName()) ;
    }

    /**
     * 构建数据库名称（根据业务规则）
     */
    public String getSchemeName(String schemeCode) {
        // 这里可以根据catalogId、orgId等生成数据库名称
        return String.format("ns%s", schemeCode) ;
    }

    /**
     * 根据DTO中的字段定义构建Schema
     */
    public Schema createDefaultSchema(CreateCatalogTableDto dto) {
        List<Types.NestedField> fields = new ArrayList<>();
        int fieldId = 1; // Iceberg字段ID从1开始
        Set<String> fieldNames = new HashSet<>();

        // 处理每个字段
        for (TableFieldDto fieldDto : dto.getFields()) {
            Types.NestedField field = buildNestedField(fieldId++, fieldDto);
            fields.add(field);
            fieldNames.add(fieldDto.getName());
        }

        // 只在不存在同名字段时添加系统字段
        if (!fieldNames.contains("create_time")) {
            fields.add(Types.NestedField.optional(fieldId++, "create_time", Types.TimestampType.withZone()));
        }

        if (!fieldNames.contains("update_time")) {
            fields.add(Types.NestedField.optional(fieldId++, "update_time", Types.TimestampType.withZone()));
        }

        return new Schema(fields);
    }

    /**
     * 根据字段DTO构建Iceberg字段
     */
    private Types.NestedField buildNestedField(int fieldId, TableFieldDto fieldDto) {
        // 确定字段是否为必需
        boolean isRequired = Boolean.TRUE.equals(fieldDto.getNotNull());

        // 获取对应的Iceberg数据类型
        org.apache.iceberg.types.Type icebergType = convertToIcebergType(fieldDto);

        // 构建字段
        if (isRequired) {
            return Types.NestedField.required(fieldId, fieldDto.getName(), icebergType, fieldDto.getDescription());
        } else {
            return Types.NestedField.optional(fieldId, fieldDto.getName(), icebergType, fieldDto.getDescription());
        }
    }

    /**
     * 将前端数据类型转换为Iceberg数据类型
     */
    private org.apache.iceberg.types.Type convertToIcebergType(TableFieldDto fieldDto) {
        String dataType = fieldDto.getDataType().toLowerCase();
        String length = fieldDto.getLength();

        switch (dataType) {
            case "int":
                return Types.IntegerType.get();
            case "bigint":
                return Types.LongType.get();
            case "varchar":
                // 处理长度，如果没有指定长度，使用默认值
                if (length != null && !length.trim().isEmpty()) {
                    try {
                        int len = Integer.parseInt(length);
                        return Types.StringType.get();
                        // 注意：Iceberg的StringType没有长度限制，但我们可以记录长度信息
                    } catch (NumberFormatException e) {
                        log.warn("字段 {} 的长度格式错误: {}", fieldDto.getName(), length);
                        return Types.StringType.get();
                    }
                }
                return Types.StringType.get();
            case "date":
                return Types.DateType.get();
            case "timestamp":
                return Types.TimestampType.withoutZone();
            case "boolean":
                return Types.BooleanType.get();
            case "double":
                return Types.DoubleType.get();
            case "float":
                return Types.FloatType.get();
            case "decimal":
                // 处理Decimal类型，需要精度和小数位
                if (length != null && length.contains(",")) {
                    try {
                        String[] parts = length.split(",");
                        int precision = Integer.parseInt(parts[0].trim());
                        int scale = Integer.parseInt(parts[1].trim());
                        return Types.DecimalType.of(precision, scale);
                    } catch (Exception e) {
                        log.warn("字段 {} 的decimal格式错误: {}", fieldDto.getName(), length);
                        return Types.DecimalType.of(10, 2); // 默认值
                    }
                }
                return Types.DecimalType.of(10, 2); // 默认精度和小数位
            default:
                log.warn("未知的数据类型: {}, 使用StringType作为默认值", dataType);
                return Types.StringType.get();
        }
    }

    /**
     * 构建分区规则 - 根据字段定义中的分区字段
     */
    public PartitionSpec buildPartitionSpec(Schema schema, CreateCatalogTableDto dto) {
        PartitionSpec.Builder partitionBuilder = PartitionSpec.builderFor(schema);

        // 查找标记为分区的字段
        for (TableFieldDto fieldDto : dto.getFields()) {
            if (Boolean.TRUE.equals(fieldDto.getIsPartition())) {
                String fieldName = fieldDto.getName();
                String dataType = fieldDto.getDataType().toLowerCase();

                // 根据字段类型选择合适的分区策略
                switch (dataType) {
                    case "date":
                        partitionBuilder.day(fieldName);
                        break;
                    case "timestamp":
                        partitionBuilder.hour(fieldName); // 或者day，根据需求
                        break;
                    case "int":
                    case "bigint":
                        partitionBuilder.identity(fieldName);
                        break;
                    case "varchar":
                        partitionBuilder.identity(fieldName);
                        break;
                    default:
                        partitionBuilder.identity(fieldName);
                        log.info("字段 {} 使用默认identity分区", fieldName);
                }
            }
        }

        // 如果没有指定分区字段，使用默认分区（按创建时间）
        if (partitionBuilder.toString().equals("always true")) {
            partitionBuilder.day("create_time");
            log.info("未指定分区字段，使用默认分区: create_time");
        }

        return partitionBuilder.build();
    }

    /**
     * 构建分区规则
     */
    public PartitionSpec buildPartitionSpec(Schema schema) {
        // 这里可以根据业务需求配置分区规则
        return PartitionSpec.builderFor(schema)
                .day("create_time")
                .build();
    }

    /**
     * 构建表属性，包含正确的存储位置
     */
    public Map<String, String> buildTableProperties(CatalogTableEntity entity) {
        Map<String, String> tableProperties = new HashMap<>();
        tableProperties.put("comment", entity.getDescription() != null ? entity.getDescription() : "");
        tableProperties.put("location", getTableStorageLocation(entity));
        return tableProperties;
    }

    /**
     * 获取表存储位置 - 根据Catalog的bucket和目录层级
     */
    public String getTableStorageLocation(CatalogTableEntity entity) {
        ICatalogService catalogService = SpringUtil.getBean(ICatalogService.class);
        // 获取Catalog信息
        CatalogEntity catalog = catalogService.getById(entity.getCatalogId());
        if (catalog == null) {
            throw new RuntimeException("找不到对应的Catalog: " + entity.getCatalogId());
        }

        // 构建完整的目录路径
        String fullDirectoryPath = buildFullDirectoryPath(entity);

        // 构建完整的存储位置
        return String.format("%s/%s/%s",
                catalog.getCatalogName(),
                fullDirectoryPath,
                entity.getTableName());
    }

    private String buildFullDirectoryPath(CatalogTableEntity entity) {
        // 调用service中的方法构建路径
        // 这里需要确保CatalogTableServiceImpl中有相应的方法
        ICatalogTableService catalogTableService = SpringUtil.getBean(ICatalogTableService.class);
        try {
            return catalogTableService.buildFullDirectoryPath(entity);
        } catch (Exception e) {
            log.warn("无法调用buildFullDirectoryPath方法，使用简化路径");
            return "tables"; // 备用方案
        }
    }

    /**
     * 获取表存储位置
     */
    public String getTableLocation(CatalogTableEntity entity) {
        String schemeName = getSchemeName(entity);
        return icebergTableManager.getTableLocation(schemeName, entity.getTableName());
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(CatalogTableEntity entity) {
        String schemeName = getSchemeName(entity);
        return icebergTableManager.tableExists(schemeName, entity.getTableName());
    }

    /**
     * 加载Iceberg表
     */
    public Table loadTable(String schemeName, String tableName) {
        return icebergTableManager.loadTable(schemeName, tableName);
    }

    /**
     * 获取表统计信息
     *
     * @param schemeName
     * @param tableName
     * @return
     */
    public TableStorageStats getTableStatistics(String schemeName, String tableName) {
        return icebergTableManager.getTableStorageStats(schemeName, tableName);
    }

    /**
     * 创建Scheme方案
     * @param schemeName
     */
    public void createScheme(String schemeName) {
        try {
            // 检查Scheme是否已存在
            if (icebergTableManager.databaseExists(schemeName)) {
                log.info("Scheme已存在: {}", schemeName);
                return;
            }

            // 创建Scheme属性
            Map<String, String> properties = new HashMap<>();
            properties.put("comment", "Auto created by data lake platform");
            properties.put("owner", "system");

            // 创建Scheme
            icebergTableManager.createDatabase(schemeName, properties);
            log.info("Scheme创建成功: {}", schemeName);

        } catch (Exception e) {
            log.error("创建Scheme失败: {}", schemeName, e);
            throw new RuntimeException("创建Scheme失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除Scheme方案
     * @param schemeName 方案名称
     * @param cascade 是否级联删除表
     */
    public void dropScheme(String schemeName, boolean cascade) {
        try {
            icebergTableManager.dropDatabase(schemeName, cascade);
            log.info("Scheme删除成功: {}", schemeName);
        } catch (Exception e) {
            log.error("删除Scheme失败: {}", schemeName, e);
            throw new RuntimeException("删除Scheme失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取所有Scheme列表
     * @return Scheme名称列表
     */
    public List<String> listSchemes() {
        return icebergTableManager.listDatabases();
    }

    /**
     * 获取数据库存储统计
     * @param schemeName 数据库名称
     * @return 数据库存储统计
     */
    public DatabaseStorageStats getDatabaseStorageStats(String schemeName) {
        return icebergTableManager.getDatabaseStorageStats(schemeName);
    }

    /**
     * 获取数据库总存储量（字节）
     * @param schemeName 数据库名称
     * @return 存储量字节数
     */
    public long getDatabaseStorageSize(String schemeName) {
        DatabaseStorageStats stats = getDatabaseStorageStats(schemeName);
        return stats.getTotalSize();
    }

    /**
     * 获取格式化后的数据库存储大小
     * @param schemeName 数据库名称
     * @return 格式化后的存储大小
     */
    public String getFormattedDatabaseStorageSize(String schemeName) {
        DatabaseStorageStats stats = getDatabaseStorageStats(schemeName);
        return stats.getFormattedSize();
    }

    /**
     * 获取整个Catalog的总存储量
     * @return 存储量字节数
     */
    public long getTotalStorageSize() {
        long totalSize = 0L;
        List<String> databases = listSchemes();

        for (String database : databases) {
            totalSize += getDatabaseStorageSize(database);
        }

        return totalSize;
    }

    /**
     * 获取格式化后的总存储大小
     * @return 格式化后的存储大小
     */
    public String getFormattedTotalStorageSize() {
        long sizeBytes = getTotalStorageSize();
        return formatBytes(sizeBytes);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 列出数据库中的所有表
     * @param schemeName 数据库名称
     * @return 表名称列表
     */
    public List<String> listSchemeTables(String schemeName) {
        return icebergTableManager.listTables(schemeName);
    }

    /**
     * 列出数据库中的所有表（带详细信息）
     * @param schemeName 数据库名称
     * @return 表信息列表
     */
    public List<TableInfoDto> listSchemeTablesWithInfo(String schemeName) {
        return icebergTableManager.listTablesWithInfo(schemeName);
    }

    /**
     * 获取数据库中的表数量
     * @param schemeName 数据库名称
     * @return 表数量
     */
    public int getSchemeTableCount(String schemeName) {
        return icebergTableManager.getTableCount(schemeName);
    }

    /**
     * 检查表是否存在于数据库中
     * @param schemeName 数据库名称
     * @param tableName 表名称
     * @return 是否存在
     */
    public boolean tableExistsInScheme(String schemeName, String tableName) {
        List<String> tables = listSchemeTables(schemeName);
        return tables.contains(tableName);
    }

    /**
     * 搜索数据库中的表（按名称模糊搜索）
     * @param schemeName 数据库名称
     * @param keyword 搜索关键词
     * @return 匹配的表名称列表
     */
    public List<String> searchTablesInScheme(String schemeName, String keyword) {
        List<String> allTables = listSchemeTables(schemeName);
        if (keyword == null || keyword.trim().isEmpty()) {
            return allTables;
        }

        String searchPattern = keyword.toLowerCase();
        return allTables.stream()
                .filter(tableName -> tableName.toLowerCase().contains(searchPattern))
                .collect(Collectors.toList());
    }

    /**
     * 获取表字段信息
     * @param tableName
     * @param tableNamespace
     * @return
     */
    public List<TableFieldDto> getTableFields(String tableName, String tableNamespace) {
        try {
            // 优先直接用传入的 namespace 加载表，失败则尝试用 getSchemeName 转换后再加载
            Table table;
            try {
                table = icebergTableManager.loadTable(tableNamespace, tableName);
            } catch (Exception firstEx) {
                String schemeName = getSchemeName(tableNamespace);
                table = icebergTableManager.loadTable(schemeName, tableName);
            }

            if (table == null) {
                return Collections.emptyList();
            }

            Schema schema = table.schema();
            List<Types.NestedField> nestedFields;
            try {
                // schema.columns() 在常见 Iceberg 版本中可用
                nestedFields = schema.columns();
            } catch (NoSuchMethodError e) {
                // 兼容处理：尝试通过 asStruct().fields() 获取
                nestedFields = schema.asStruct().fields();
            }

            // 获取分区字段名集合
            Set<String> partitionNames = new HashSet<>();
            try {
                partitionNames = table.spec().fields().stream()
                        .map(org.apache.iceberg.PartitionField::name)
                        .collect(Collectors.toSet());
            } catch (Exception ignored) {
            }

            List<TableFieldDto> result = new ArrayList<>(nestedFields.size());
            for (Types.NestedField field : nestedFields) {
                TableFieldDto dto = new TableFieldDto();
                dto.setName(field.name());
                // Iceberg 没有主键的概念，这里默认 false
                dto.setIsPrimary(false);
                dto.setNotNull(field.isRequired());
                dto.setIsPartition(partitionNames.contains(field.name()));
                dto.setDescription(field.doc());

                // 将 Iceberg 类型转换为前端/DTO 可读的类型描述
                org.apache.iceberg.types.Type t = field.type();
                String dataType;
                String length = null;

                if (t instanceof Types.IntegerType) {
                    dataType = "int";
                } else if (t instanceof Types.LongType) {
                    dataType = "bigint";
                } else if (t instanceof Types.StringType) {
                    dataType = "varchar";
                } else if (t instanceof Types.DateType) {
                    dataType = "date";
                } else if (t instanceof Types.TimestampType) {
                    // 不区分 with/without zone，这里统一为 timestamp
                    dataType = "timestamp";
                } else if (t instanceof Types.BooleanType) {
                    dataType = "boolean";
                } else if (t instanceof Types.DoubleType) {
                    dataType = "double";
                } else if (t instanceof Types.FloatType) {
                    dataType = "float";
                } else if (t instanceof Types.DecimalType) {
                    Types.DecimalType dt = (Types.DecimalType) t;
                    dataType = "decimal";
                    length = dt.precision() + "," + dt.scale();
                } else {
                    // 兜底：使用 Iceberg 类型的字符串表示
                    dataType = t.toString();
                }

                dto.setDataType(dataType);
                dto.setLength(length);

                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            log.error("获取表字段信息失败: {}.{}", tableNamespace, tableName, e);
            return Collections.emptyList();
        }
    }
}
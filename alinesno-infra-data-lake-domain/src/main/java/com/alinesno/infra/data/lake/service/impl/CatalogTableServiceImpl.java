package com.alinesno.infra.data.lake.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.lake.api.CreateCatalogTableDto;
import com.alinesno.infra.data.lake.api.TableFieldDto;
import com.alinesno.infra.data.lake.api.iceberg.TableStorageStats;
import com.alinesno.infra.data.lake.entity.CatalogTableEntity;
import com.alinesno.infra.data.lake.enums.DirectoryLevel;
import com.alinesno.infra.data.lake.handle.IcebergTableUtils;
import com.alinesno.infra.data.lake.mapper.CatalogTableMapper;
import com.alinesno.infra.data.lake.service.ICatalogTableService;
import com.alinesno.infra.data.lake.service.IStorageFileService;
import com.alinesno.infra.data.lake.utils.InitRawMetaUtils;
import com.alinesno.infra.data.lake.utils.SyncTableUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.Table;
import org.apache.iceberg.types.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 数据目录服务实现
 */
@Slf4j
@Service
public class CatalogTableServiceImpl extends IBaseServiceImpl<CatalogTableEntity, CatalogTableMapper> implements ICatalogTableService {

    @Autowired
    private IStorageFileService storageFileService;

    @Autowired
    private IcebergTableUtils icebergTableUtils;

    @Autowired
    private SyncTableUtils syncTableUtils ;

    /**
     * 初始化数据湖目录结构
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeDataLakeStructure(long catalogId , long orgId) {

        log.info("开始初始化数据湖目录结构...");

        // 检查是否已初始化，避免重复创建
        LambdaQueryWrapper<CatalogTableEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CatalogTableEntity::getCatalogId, catalogId);
        queryWrapper.eq(CatalogTableEntity::getOrgId, orgId);

        long count = count(queryWrapper);
        if (count > 0) {
            log.warn("数据湖目录结构已存在，跳过初始化");
            return;
        }

        // 创建顶层目录
        for (DirectoryLevel level : DirectoryLevel.values()) {
            CatalogTableEntity directory = new CatalogTableEntity();
            directory.setTableName(level.getName());
            directory.setDescription(level.getDescription());
            directory.setDirectory(true);
            directory.setCatalogId(catalogId);
            directory.setParentId(0L); // 0表示顶级目录
            directory.setOrgId(orgId);

            // 每个层级创建对应的数据库
            String schemeCode = IdUtil.getSnowflakeNextIdStr() ;
            icebergTableUtils.createScheme(icebergTableUtils.getSchemeName(schemeCode));
            directory.setSchemeName(schemeCode);

            baseMapper.insert(directory);

            log.info("创建目录: {} - {}", level.getName(), level.getDescription());

            // 为raw层创建元数据表
            if (DirectoryLevel.RAW.equals(level)) {
                CatalogTableEntity metaTable = new CatalogTableEntity();
                metaTable.setTableName("raw_metadata");
                metaTable.setDescription("原始数据元数据表，管理非结构化数据的元信息（如Oss存储的文件）");
                metaTable.setDirectory(false);
                metaTable.setFormatType("METADATA");
                metaTable.setParentId(directory.getId()); // 关联到raw目录
                metaTable.setCatalogId(catalogId);
                metaTable.setOrgId(orgId);
                metaTable.setSchemeName(schemeCode);

                CreateCatalogTableDto metaTableDto = InitRawMetaUtils.getCreateCatalogTableDto(catalogId, directory);

                // 创建元数据表
                createIcebergTable(metaTable, metaTableDto);
                log.info("在raw目录下创建元数据表: raw_metadata");
            }
        }

        log.info("数据湖目录结构初始化完成");
    }

    private void createIcebergTable(CatalogTableEntity entity , CreateCatalogTableDto dto) {

        // 检查名称是否已存在
        if (checkNameExists(entity.getTableName(), entity.getCatalogId(), entity.getParentId(), entity.getOrgId())) {
            throw new RuntimeException("名称已存在: " + entity.getTableName());
        }

        save(entity);

        if (!entity.isDirectory()) {  // 创建表结构
            // 创建表，则在Iceberg创建表结构，并指定表存储位置
            try {
                // 使用工具类创建Iceberg表
                Table table = icebergTableUtils.createIcebergTable(entity , dto);

                // 更新表的存储位置信息
                entity.setStorageLocation(table.location());
                updateById(entity);

                log.info("Iceberg表创建成功，存储位置: {}", table.location());

            } catch (Exception e) {
                log.error("创建Iceberg表失败: {}", entity.getTableName(), e);
                // 回滚：删除已插入的数据库记录
                removeById(entity.getId());
                throw new RuntimeException("创建Iceberg表失败: " + e.getMessage(), e);
            }
        }
    }


    /**
     * 获取目录树形结构
     */
    @Override
    public List<Map<String, Object>> getCatalogTree(long catalogId, long orgId) {
        log.info("获取目录树形结构, catalogId: {}, orgId: {}", catalogId, orgId);

        // 查询该目录下的所有节点
        LambdaQueryWrapper<CatalogTableEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CatalogTableEntity::getCatalogId, catalogId);
        queryWrapper.eq(CatalogTableEntity::getOrgId, orgId);
        queryWrapper.orderByAsc(CatalogTableEntity::getId);

        List<CatalogTableEntity> allNodes = baseMapper.selectList(queryWrapper);

        // 转换为树形结构
        return buildTree(allNodes, 0L);
    }

    /**
     * 递归构建树形结构
     */
    private List<Map<String, Object>> buildTree(List<CatalogTableEntity> allNodes, Long parentId) {
        List<Map<String, Object>> treeNodes = new ArrayList<>();

        // 筛选出父节点为parentId的节点
        List<CatalogTableEntity> children = allNodes.stream()
                .filter(node -> parentId.equals(node.getParentId()))
                .toList();

        // 递归处理每个子节点
        for (CatalogTableEntity child : children) {
            Map<String, Object> node = new HashMap<>();

            node.put("id", child.getId());  // 使用ID作为节点ID
            node.put("label", child.getTableName());
            node.put("isLeaf", !child.isDirectory());
            node.put("isDirectory", child.isDirectory());
            node.put("parentId", child.getParentId());
            node.put("desc", child.getDescription() != null ?  child.getDescription() : "");

            // 递归获取子节点
            List<Map<String, Object>> subNodes = buildTree(allNodes, child.getId());
            if (!subNodes.isEmpty()) {
                node.put("children", subNodes);
            }

            treeNodes.add(node);
        }

        return treeNodes;
    }

    /**
     * 检查名称是否已存在
     */
    @Override
    public boolean checkNameExists(String tableName, long catalogId, long parentId, long orgId) {
        LambdaQueryWrapper<CatalogTableEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CatalogTableEntity::getTableName, tableName);
        queryWrapper.eq(CatalogTableEntity::getCatalogId, catalogId);
        queryWrapper.eq(CatalogTableEntity::getParentId, parentId);
        queryWrapper.eq(CatalogTableEntity::getOrgId, orgId);

        return count(queryWrapper) > 0;
    }

    @Override
    public void createDirectory(CatalogTableEntity entity) {

        // 检查名称是否已存在
        if (checkNameExists(entity.getTableName(), entity.getCatalogId(),  entity.getParentId(), entity.getOrgId())) {
            throw new RuntimeException("名称已存在: " + entity.getTableName());
        }

        // 每个层级创建对应的数据库
        String schemeCode = IdUtil.getSnowflakeNextIdStr() ;
        icebergTableUtils.createScheme(icebergTableUtils.getSchemeName(schemeCode));
        entity.setSchemeName(schemeCode);

        save(entity);
    }

    /**
     * 构建完整的目录路径（递归获取所有父级目录）
     */
    @Override
    public String buildFullDirectoryPath(CatalogTableEntity entity) {
        List<String> pathSegments = new ArrayList<>();

        // 添加当前目录/表名
        if (entity.isDirectory()) {
            pathSegments.add(entity.getTableName());
        }

        // 递归获取所有父级目录名称
        Long currentParentId = entity.getParentId();
        while (currentParentId != null && currentParentId != 0L) {
            CatalogTableEntity parentEntity = getById(currentParentId);
            if (parentEntity != null && parentEntity.isDirectory()) {
                pathSegments.add(0, parentEntity.getTableName());
                currentParentId = parentEntity.getParentId();
            } else {
                break;
            }
        }

        // 构建完整路径
        return String.join("/", pathSegments);
    }

    /**
     * 获取Bucket后缀（根据业务逻辑确定）
     */
    private String getBucketSuffix(CatalogTableEntity entity) {
        // 这里可以根据实际业务需求确定Bucket后缀
        // 例如：使用catalogId、orgId或其他标识
        return String.valueOf(entity.getCatalogId());
    }

    /**
     * 删除目录（包括OSS中的对应目录）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDirectory(Long id) {
        CatalogTableEntity entity = getById(id);
        if (entity == null) {
            throw new RuntimeException("目录不存在");
        }

        if (!entity.isDirectory()) {
            throw new RuntimeException("不是目录类型");
        }

        // 检查是否有子节点
        LambdaQueryWrapper<CatalogTableEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CatalogTableEntity::getParentId, id);
        long childCount = count(queryWrapper);

        if (childCount > 0) {
            throw new RuntimeException("目录不为空，无法删除");
        }

        // 删除OSS中的目录
        String fullPath = buildFullDirectoryPath(entity);
        storageFileService.deleteDirectory(getBucketSuffix(entity), fullPath);

        // 删除数据库记录
        removeById(id);
        log.info("删除目录成功: {}", fullPath);
    }

    @Override
    public void renameDirectory(Long id, String newName) {

    }

    @Override
    public void createTable(CreateCatalogTableDto dto) {
        CatalogTableEntity metaTable = new CatalogTableEntity();

        metaTable.setTableName(dto.getTableName());
        metaTable.setDescription(dto.getDescription());
        metaTable.setDirectory(false);
        metaTable.setFormatType(dto.getFormat());

        CatalogTableEntity catalogTable = getById(dto.getParentId());
        metaTable.setSchemeName(catalogTable.getSchemeName());

        metaTable.setParentId(dto.getParentId()); // 关联到raw目录
        metaTable.setOperatorId(dto.getOperatorId());
        metaTable.setCatalogId(dto.getCatalogId());
        metaTable.setOrgId(dto.getOrgId());

        // 创建元数据表
        createIcebergTable(metaTable , dto);
    }

    @Override
    public List<TableFieldDto> getTableFields(CatalogTableEntity entity) {
        try {
            // 检查是否为表而不是目录
            if (entity.isDirectory()) {
                log.warn("实体 {} 是目录，不是表", entity.getTableName());
                return new ArrayList<>();
            }

            // 获取Iceberg表
            String schemeName = icebergTableUtils.getSchemeName(entity);
            Table table = icebergTableUtils.loadTable(schemeName, entity.getTableName());

            if (table == null) {
                log.warn("表不存在: {}.{}", schemeName, entity.getTableName());
                return new ArrayList<>();
            }

            // 获取表的分区信息
            Set<String> partitionFields = new HashSet<>();
            if (table.spec() != null && !table.spec().isUnpartitioned()) {
                table.spec().fields().forEach(partitionField -> {
                    partitionFields.add(partitionField.name());
                });
            }

            // 转换字段信息
            List<TableFieldDto> fieldDtos = new ArrayList<>();
            table.schema().columns().forEach(column -> {
                TableFieldDto dto = convertToTableFieldDto(column, partitionFields);
                fieldDtos.add(dto);
            });

            return fieldDtos;

        } catch (Exception e) {
            log.error("获取表字段信息失败: {}", entity.getTableName(), e);
            throw new RuntimeException("获取表字段信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将Iceberg字段转换为TableFieldDto
     */
    private TableFieldDto convertToTableFieldDto(Types.NestedField column, Set<String> partitionFields) {
        TableFieldDto dto = new TableFieldDto();

        dto.setName(column.name());
        dto.setDataType(convertIcebergTypeToString(column.type()));
        dto.setDescription(column.doc());
        dto.setNotNull(!column.isOptional());

        // 检查是否为分区字段
        dto.setIsPartition(partitionFields.contains(column.name()));

        // 对于字符串类型，可以设置长度（如果需要）
        if (column.type().typeId() == Types.StringType.get().typeId()) {
            dto.setLength("255"); // 默认长度，可以根据需要调整
        }

        // 对于Decimal类型，提取精度和小数位
        if (column.type() instanceof Types.DecimalType) {
            Types.DecimalType decimalType = (Types.DecimalType) column.type();
            dto.setLength(decimalType.precision() + "," + decimalType.scale());
        }

        return dto;
    }

    /**
     * 将Iceberg类型转换为字符串表示
     */
    private String convertIcebergTypeToString(org.apache.iceberg.types.Type type) {
        switch (type.typeId()) {
            case BOOLEAN:
                return "boolean";
            case INTEGER:
                return "int";
            case LONG:
                return "bigint";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            case DATE:
                return "date";
            case TIME:
            case TIMESTAMP:
                if (type instanceof Types.TimestampType timestampType) {
                    return timestampType.shouldAdjustToUTC() ? "timestamptz" : "timestamp";
                }
                return "timestamp";
            case STRING:
                return "varchar";
            case UUID:
                return "uuid";
            case DECIMAL:
                Types.DecimalType decimalType = (Types.DecimalType) type;
                return "decimal(" + decimalType.precision() + "," + decimalType.scale() + ")";
            case FIXED, BINARY:
                return "binary";
            case STRUCT:
            case LIST:
            case MAP:
                // 复杂类型
                return type.toString();
            default:
                return type.toString();
        }
    }

    @Override
    public TableStorageStats getTableStatistics(CatalogTableEntity entity) {
        // 检查是否为表而不是目录
        if (entity.isDirectory()) {
            log.warn("实体 {} 是目录，不是表", entity.getTableName());
            return new TableStorageStats();
        }

        String schemeName = icebergTableUtils.getSchemeName(entity);
        return icebergTableUtils.getTableStatistics(schemeName, entity.getTableName());

    }

    @Override
    public CatalogTableEntity getRawMetadataTable(Long catalogId) {

        LambdaQueryWrapper<CatalogTableEntity> queryWrapper = new LambdaQueryWrapper<>() ;
        queryWrapper.eq(CatalogTableEntity::getCatalogId, catalogId) ;

        List<CatalogTableEntity> tables = list(queryWrapper);

        for (CatalogTableEntity table : tables) {
            if (DirectoryLevel.RAW.getName().equals(table.getTableName())) {
                return table;
            }
        }

        return null;
    }

    @Override
    public void syncTableStructure(Long catalogId) {
        syncTableUtils.syncCatalogTable(catalogId);
    }

}

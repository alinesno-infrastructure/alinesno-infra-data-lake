package com.alinesno.infra.data.lake.service;


import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.lake.api.CreateCatalogTableDto;
import com.alinesno.infra.data.lake.api.TableFieldDto;
import com.alinesno.infra.data.lake.api.iceberg.TableStorageStats;
import com.alinesno.infra.data.lake.entity.CatalogTableEntity;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author LuoXiaoDong
 * @version 1.0.0
 */

public interface ICatalogTableService extends IBaseService<CatalogTableEntity> {

    /**
     * 初始化数据湖目录结构
     */
    void initializeDataLakeStructure(long catalogId , long orgId);

    /**
     * 获取目录树形结构
     */
    List<Map<String, Object>> getCatalogTree(long catalogId, long orgId);

    /**
     * 检查名称是否已存在
     */
    boolean checkNameExists(String tableName, long catalogId, long parentId, long orgId);

    /**
     * 创建表或目录
     * @param entity
     */
    void createDirectory(CatalogTableEntity entity);

    /**
     * 构建完整的目录路径（递归获取所有父级目录）
     */
    String buildFullDirectoryPath(CatalogTableEntity entity);

    /**
     * 删除目录
     * @param id
     */
    void deleteDirectory(Long id);

    /**
     * 重命名目录
     * @param id
     * @param newName
     */
    void renameDirectory(Long id, String newName);

    /**
     * 创建表
     * @param dto
     */
    void createTable(CreateCatalogTableDto dto);

    /**
     * 获取Iceberg表字段
     * @param entity
     * @return
     */
    List<TableFieldDto> getTableFields(CatalogTableEntity entity);

    /**
     * 获取表统计信息
     * @param entity 表实体
     * @return 表统计信息
     */
    TableStorageStats getTableStatistics(CatalogTableEntity entity);

    /**
     * 获取原始元数据表
     * @param catalogId
     * @return
     */
    CatalogTableEntity getRawMetadataTable(Long catalogId);

    /**
     * 同步表结构
     * @param catalogId
     */
    void syncTableStructure(Long catalogId);
}

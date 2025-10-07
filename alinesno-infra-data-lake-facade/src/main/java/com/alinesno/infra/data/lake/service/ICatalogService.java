package com.alinesno.infra.data.lake.service;


import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.lake.api.CatalogDto;
import com.alinesno.infra.data.lake.api.iceberg.LakeCountStatsDto;
import com.alinesno.infra.data.lake.entity.CatalogEntity;
import jakarta.validation.Valid;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author LuoXiaoDong
 * @version 1.0.0
 */

public interface ICatalogService extends IBaseService<CatalogEntity> {

    /**
     * 创建目录
     * @param catalogDto
     * @return
     */
    CatalogEntity createCatalog(@Valid CatalogDto catalogDto);

    /**
     * 删除目录
     * @param id
     */
    void deleteCatalog(Long id);

    /**
     * 统计
     * @return
     */
    LakeCountStatsDto countStats();

    /**
     * 获取到最新的8个数据目录
     *
     * @param permissionQuery
     * @param count
     * @return
     */
    List<CatalogEntity> latestCatalogs(PermissionQuery permissionQuery, int count);
}

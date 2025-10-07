package com.alinesno.infra.data.lake.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.data.lake.api.CatalogDto;
import com.alinesno.infra.data.lake.api.iceberg.LakeCountStatsDto;
import com.alinesno.infra.data.lake.entity.CatalogEntity;
import com.alinesno.infra.data.lake.handle.IcebergTableUtils;
import com.alinesno.infra.data.lake.mapper.CatalogMapper;
import com.alinesno.infra.data.lake.service.ICatalogService;
import com.alinesno.infra.data.lake.service.ICatalogTableService;
import com.alinesno.infra.data.lake.service.IStorageFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据目录服务实现
 */
@Slf4j
@Service
public class CatalogServiceImpl extends IBaseServiceImpl<CatalogEntity, CatalogMapper> implements ICatalogService {

    @Autowired
    private IStorageFileService storageFileService;

    @Autowired
    private IcebergTableUtils icebergTableUtils ;

    @Autowired
    private ICatalogTableService catalogTableService;

    @Override
    public CatalogEntity createCatalog(CatalogDto catalogDto) {

        CatalogEntity catalogEntity = new CatalogEntity();
        BeanUtils.copyProperties(catalogDto, catalogEntity);

        // 设置默认值
        catalogEntity.setTableCount(0);
        catalogEntity.setStorageSize(0.0);
        catalogEntity.setStorageUnit("MB");
        catalogEntity.setStatus("活跃");

        save(catalogEntity);

//        String schemeName = icebergTableUtils.getSchemeName(String.valueOf(catalogEntity.getId())) ; // "database_" + catalogEntity.getId() ;
//        icebergTableUtils.createScheme(schemeName);
//        catalogEntity.setSchemeName(schemeName);

        catalogTableService.initializeDataLakeStructure(catalogEntity.getId(), catalogEntity.getOrgId());

        update(catalogEntity);

        return catalogEntity ;
    }

    @Override
    public void deleteCatalog(Long id) {
       // 先删除bucket
        CatalogEntity catalogEntity = getById(id) ;
        storageFileService.deleteBucket(String.valueOf(catalogEntity.getId()));

        removeById(id) ;
    }

    @Override
    public LakeCountStatsDto countStats() {

        LakeCountStatsDto lakeCountStatsDto = new LakeCountStatsDto();

        String formattedTotalStorageSize = icebergTableUtils.getFormattedTotalStorageSize() ;
        long schemeCount = icebergTableUtils.listSchemes().size() ;
        long schemeTableCount = icebergTableUtils.listSchemes().stream()
                .mapToInt(icebergTableUtils::getSchemeTableCount)
                .sum();

        lakeCountStatsDto.setTotalStorageSize(formattedTotalStorageSize);
        lakeCountStatsDto.setSchemeCount(schemeCount);
        lakeCountStatsDto.setSchemeTableCount(schemeTableCount);

        return lakeCountStatsDto;
    }

    @Override
    public List<CatalogEntity> latestCatalogs(PermissionQuery permissionQuery, int count) {

        LambdaQueryWrapper<CatalogEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.setEntityClass(CatalogEntity.class) ;
        permissionQuery.toWrapper(queryWrapper);
        queryWrapper.orderByDesc(CatalogEntity::getAddTime) ;
        queryWrapper.last("LIMIT " + count) ;

        return list(queryWrapper) ;
    }

}
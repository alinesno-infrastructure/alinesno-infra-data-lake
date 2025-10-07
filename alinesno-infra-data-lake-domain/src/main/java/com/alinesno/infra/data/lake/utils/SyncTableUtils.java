package com.alinesno.infra.data.lake.utils;

import cn.hutool.json.JSONUtil;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.common.facade.base.BaseDto;
import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.lake.api.CreateCatalogTableDto;
import com.alinesno.infra.data.lake.api.TableFieldDto;
import com.alinesno.infra.data.lake.api.TablePropertyDto;
import com.alinesno.infra.data.lake.entity.CatalogEntity;
import com.alinesno.infra.data.lake.entity.CatalogTableEntity;
import com.alinesno.infra.data.lake.entity.IcebergTablesEntity;
import com.alinesno.infra.data.lake.handle.IcebergTableManager;
import com.alinesno.infra.data.lake.handle.IcebergTableUtils;
import com.alinesno.infra.data.lake.mapper.IcebergTablesMapper;
import com.alinesno.infra.data.lake.service.ICatalogService;
import com.alinesno.infra.data.lake.service.ICatalogTableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.projectnessie.model.IcebergTable;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步表工具类
 */
@Slf4j
@Component
public class SyncTableUtils {

    @Autowired
    private IcebergTablesMapper icebergTablesMapper;

    @Autowired
    private IcebergTableUtils icebergTableUtils;

    /**
     * 同步表
     * @param catalogId
     */
    public void syncCatalogTable(Long catalogId) {

        ICatalogService catalogService = SpringUtils.getBean(ICatalogService.class);
        ICatalogTableService catalogTableService = SpringUtils.getBean(ICatalogTableService.class);

        CatalogEntity catalogEntity = catalogService.getById(catalogId) ;

        // 查询目录下的所有表信息(只为目录结构)
        LambdaQueryWrapper<CatalogTableEntity> catalogQueryWrapper = new LambdaQueryWrapper<>();
        catalogQueryWrapper.eq(CatalogTableEntity::getCatalogId, catalogId);
        List<CatalogTableEntity> catalogTableEntities = catalogTableService.list(catalogQueryWrapper);

        // 过滤出目录结构
        List<CatalogTableEntity> catalogTableDirectoryEntities = catalogTableEntities.stream().filter(CatalogTableEntity::isDirectory).toList();

        // 过滤出表结构
        List<CatalogTableEntity> catalogTableTableEntities = catalogTableEntities.stream().filter(catalogTableEntity -> !catalogTableEntity.isDirectory()).toList();

        for (CatalogTableEntity catalogTableEntity : catalogTableDirectoryEntities) {

            log.debug("-->>>>>>>>{}:{}>>>>>>>>>>>>>>>>>>>>>>>" , catalogTableEntity.getTableName() , catalogTableEntity.getSchemeName());

            // 查询出所有的iceberg表结构信息
            LambdaQueryWrapper<IcebergTablesEntity> icebergQueryWrapper = new LambdaQueryWrapper<>();
            icebergQueryWrapper.eq(IcebergTablesEntity::getTableNamespace, icebergTableUtils.getSchemeName(catalogTableEntity));

            List<IcebergTablesEntity> icebergTablesEntities = icebergTablesMapper.selectList(icebergQueryWrapper);
            log.debug("icebergTablesEntities 目录结构:{}", JSONUtil.toJsonPrettyStr(icebergTablesEntities));

            // 判断当前CatalogTable下面的表实体对象
            List<CatalogTableEntity> schemeTableEntities = new ArrayList<>() ;
            for (CatalogTableEntity ct : catalogTableEntities) {
                if(StringUtils.isNotEmpty(ct.getSchemeName())){
                    if(ct.getSchemeName().equals(catalogTableEntity.getSchemeName()) && !ct.isDirectory()){
                        schemeTableEntities.add(ct) ;
                    }
                }
            }

            log.debug("schemeTableEntities 目录结构:{}", JSONUtil.toJsonPrettyStr(schemeTableEntities));

            // 找出schemeTableEntities比schemeTableEntities缺少的表
            List<IcebergTablesEntity> differentTables = new ArrayList<>();
            for (IcebergTablesEntity ict : icebergTablesEntities) {
                boolean exists = false ;
                for(CatalogTableEntity schemeTableEntity : schemeTableEntities){
                   if(schemeTableEntity.getTableName().equals(ict.getTableName())) {
                       exists = true ;
                       log.debug("{}表已存在", schemeTableEntity.getTableName());
                       break;
                   }
                }
                if (!exists) {
                    differentTables.add(ict);
                }
            }

            log.debug("differentTables 目录结构:{}", JSONUtil.toJsonPrettyStr(differentTables));

            // 将缺少的表添加到schemeTableEntities表中
            for(IcebergTablesEntity diffTable : differentTables){
                CreateCatalogTableDto createCatalogTableDto = new CreateCatalogTableDto();
                BeanUtils.copyProperties(catalogEntity, createCatalogTableDto);

                createCatalogTableDto.setParentId(catalogTableEntity.getId());
                createCatalogTableDto.setCatalogId(catalogId);
                createCatalogTableDto.setTableName(diffTable.getTableName());
                createCatalogTableDto.setDescription("未添加描述");

                // 获取到表结构信息
                List<TableFieldDto> fields = icebergTableUtils.getTableFields(diffTable.getTableName(), diffTable.getTableNamespace());
                createCatalogTableDto.setFields(fields);

                // 创建同步缺少的表结构
                createSyncTable(createCatalogTableDto , catalogTableService);
            }
        }

    }

    public void createSyncTable(CreateCatalogTableDto dto , ICatalogTableService catalogTableService) {
        CatalogTableEntity metaTable = new CatalogTableEntity();

        metaTable.setTableName(dto.getTableName());
        metaTable.setDescription(dto.getDescription());
        metaTable.setDirectory(false);
        metaTable.setFormatType(dto.getFormat());

        CatalogTableEntity catalogTable = catalogTableService.getById(dto.getParentId());
        metaTable.setSchemeName(catalogTable.getSchemeName());

        metaTable.setParentId(dto.getParentId()); // 关联到raw目录
        metaTable.setOperatorId(dto.getOperatorId());
        metaTable.setCatalogId(dto.getCatalogId());
        metaTable.setOrgId(dto.getOrgId());

        catalogTableService.save(metaTable);
    }

}

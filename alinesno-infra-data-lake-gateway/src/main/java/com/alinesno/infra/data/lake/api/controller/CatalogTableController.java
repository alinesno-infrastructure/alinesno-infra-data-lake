package com.alinesno.infra.data.lake.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.lake.api.CreateCatalogTableDto;
import com.alinesno.infra.data.lake.api.TableFieldDto;
import com.alinesno.infra.data.lake.api.UpdateCatalogTableDescDto;
import com.alinesno.infra.data.lake.api.iceberg.TableStorageStats;
import com.alinesno.infra.data.lake.entity.CatalogTableEntity;
import com.alinesno.infra.data.lake.service.ICatalogTableService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处理与CatalogTableEntity相关的请求的Controller。
 */
@Slf4j
@Api(tags = "Catalog")
@Validated
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/lake/catalogTable")
public class CatalogTableController extends BaseController<CatalogTableEntity, ICatalogTableService> {

    @Autowired
    private ICatalogTableService service;

    /**
     * 获取CatalogEntity的DataTables数据。
     */
    @DataPermissionScope
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign(), page);
    }


    /**
     * 获取目录树形结构
     */
    @DataPermissionQuery
    @ApiOperation("获取目录树形结构")
    @GetMapping("/tree")
    public AjaxResult getCatalogTree(
            PermissionQuery query ,
            @RequestParam long catalogId) {
        long orgId = query.getOrgId();

        log.info("获取目录树形结构, catalogId: {}, orgId: {}", catalogId, orgId);
        return AjaxResult.success(service.getCatalogTree(catalogId, orgId));
    }

    /**
     * 创建目录
     */
    @DataPermissionSave
    @ApiOperation("创建目录")
    @PostMapping("/createDirectory")
    public AjaxResult createDirectory(@RequestBody CatalogTableEntity entity, PermissionQuery query) {
        log.info("创建目录: {}", ToStringBuilder.reflectionToString(entity));
        // 设置组织ID
        entity.setOrgId(query.getOrgId());
        // 保存实体
        service.createDirectory(entity);

        return AjaxResult.success("创建成功");
    }

    /**
     * 创建表
     */
    @DataPermissionSave
    @ApiOperation("创建表")
    @PostMapping("/createTable")
    public AjaxResult createTable(@RequestBody @Valid CreateCatalogTableDto dto , PermissionQuery query) {
        log.info("创建表: {}", ToStringBuilder.reflectionToString(dto));

        // 保存实体
        service.createTable(dto);

        return AjaxResult.success("创建成功");
    }

    /**
     * 通过id查询目录
     * @return
     */
    @ApiOperation("通过id查询目录")
    @GetMapping("/findById/{id}")
    public AjaxResult findById(@PathVariable("id") Long id) {
        log.info("通过id查询目录: {}", id);
        CatalogTableEntity entity = service.getById(id);

        AjaxResult result = AjaxResult.success(entity) ;
        // 如果是表单，则查询字段信息
        if (entity != null && !entity.isDirectory()) {
            List<TableFieldDto> fields = service.getTableFields(entity);
            TableStorageStats stats = service.getTableStatistics(entity);

            result.put("fields", fields);
            result.put("statistics", stats);
        }

        return result ;
    }

    /**
     * 同步数据表结构
     * @return
     */
    @ApiOperation("同步数据表结构")
    @GetMapping("/syncTableStructure")
    public AjaxResult syncTableStructure(@RequestParam Long catalogId) {
        log.info("同步数据表结构: {}", catalogId) ;

        service.syncTableStructure(catalogId);

        return AjaxResult.success("同步成功");
    }

    @DataPermissionSave
    @PostMapping("/updateCatalogTableDesc")
    public AjaxResult updateCatalogTableDesc(@RequestBody UpdateCatalogTableDescDto dto, PermissionQuery query) {

        log.info("更新目录/表描述: {}", ToStringBuilder.reflectionToString(dto));
        if (dto == null || dto.getId() == null) {
            return AjaxResult.error("参数缺失: id");
        }

        CatalogTableEntity entity = service.getById(dto.getId());
        entity.setDescription(dto.getDescription());

        service.updateById(entity);

        return AjaxResult.success("更新成功");
    }

    @Override
    public ICatalogTableService getFeign() {
        return this.service;
    }
}

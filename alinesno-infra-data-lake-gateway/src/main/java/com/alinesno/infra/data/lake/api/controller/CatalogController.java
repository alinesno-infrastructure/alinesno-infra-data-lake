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
import com.alinesno.infra.data.lake.api.CatalogDto;
import com.alinesno.infra.data.lake.api.iceberg.LakeCountStatsDto;
import com.alinesno.infra.data.lake.entity.CatalogEntity;
import com.alinesno.infra.data.lake.service.ICatalogService;
import com.alinesno.infra.data.lake.service.ICatalogTableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 处理与CatalogEntity相关的请求的Controller。
 */
@Slf4j
@Api(tags = "Catalog")
@Validated
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/lake/catalog")
public class CatalogController extends BaseController<CatalogEntity, ICatalogService> {

    @Autowired
    private ICatalogService service;

    @Autowired
    private ICatalogTableService tableService ;

    /**
     * 类型的统计，统计出有多少个Scheme和Table
     */
    @ApiOperation("获取类型的统计")
    @GetMapping("/countStats")
    public AjaxResult countStats() {
        LakeCountStatsDto lakeCountStatsDto = service.countStats();
        return AjaxResult.success("获取类型统计成功", lakeCountStatsDto);
    }

    /**
     * 获取组织下面最新的8个数据目录
     */
    @DataPermissionQuery
    @ApiOperation("获取组织下面最新的8个数据目录")
    @GetMapping("/latestCatalogs")
    public AjaxResult latestCatalogs(PermissionQuery  permissionQuery , int count) {
        if(count <= 0){
            count = 8 ;
        }
        return AjaxResult.success("获取组织最新8个数据目录成功", service.latestCatalogs(permissionQuery , count));
    }

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
     * 创建数据目录
     */
    @DataPermissionSave
    @ApiOperation("创建数据目录")
    @PostMapping("/createCatalog")
    public AjaxResult createCatalog(@Valid @RequestBody CatalogDto catalogDto) {
        CatalogEntity catalogEntity =  service.createCatalog(catalogDto);
        return AjaxResult.success("目录创建成功", catalogEntity);
    }

    /**
     * 更新目录
     * @return
     */
    @DataPermissionSave
    @ApiOperation("更新目录")
    @PutMapping("/updateCatalog")
    public AjaxResult updateCatalog(@Valid @RequestBody CatalogDto catalogDto) {
        try {
            CatalogEntity catalogEntity = new CatalogEntity();
            BeanUtils.copyProperties(catalogDto, catalogEntity);

            service.updateById(catalogEntity);
            return AjaxResult.success("目录更新成功", catalogEntity);
        } catch (Exception e) {
            log.error("更新目录失败", e);
            return AjaxResult.error("更新目录失败: " + e.getMessage());
        }
    }

    /**
     * 删除目录
     * @return
     */
    @DataPermissionSave
    @ApiOperation("删除目录")
    @DeleteMapping("/deleteCatalog/{id}")
    public AjaxResult deleteCatalog(@PathVariable("id") Long id) {
        service.deleteCatalog(id);
        return AjaxResult.success("目录删除成功");
    }

    /**
     * 通过id获取分类目录
     * @return
     */
    @ApiOperation("通过id获取分类目录")
    @GetMapping("/getCatalogById/{id}")
    public AjaxResult getCatalogById(@PathVariable("id") Long id) {
        CatalogEntity catalogEntity = service.getById(id);
        return AjaxResult.success("获取目录成功", catalogEntity);
    }

    /**
     * 查询所有的类型
     * @return
     */
    @DataPermissionQuery
    @ApiOperation("查询所有的类型")
    @GetMapping("/listAllCatalog")
    public AjaxResult listAllCatalog(PermissionQuery  query) {
        LambdaQueryWrapper<CatalogEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.setEntityClass(CatalogEntity.class) ;
        query.toWrapper(queryWrapper);
        queryWrapper.orderByDesc(CatalogEntity::getAddTime) ;

        return AjaxResult.success("获取所有目录成功", service.list(queryWrapper));
    }

    @Override
    public ICatalogService getFeign() {
        return this.service;
    }
}
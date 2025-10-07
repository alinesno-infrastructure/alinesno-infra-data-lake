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
import com.alinesno.infra.data.lake.entity.DomainEntity;
import com.alinesno.infra.data.lake.service.IDomainService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处理与DomainEntity相关的请求的Controller。
 * 继承自BaseController类并实现IDomainService接口。
 *
 * @author LuoXiaoDong
 * @version 1.0.0
 */
@Slf4j
@Api(tags = "Domain")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/lake/domain")
public class DomainController extends BaseController<DomainEntity, IDomainService> {

    @Autowired
    private IDomainService service;

    /**
     * 获取DomainEntity的DataTables数据。
     *
     * @param request HttpServletRequest对象。
     * @param model   Model对象。
     * @param page    DatatablesPageBean对象。
     * @return 包含DataTables数据的TableDataInfo对象。
     */
    @DataPermissionScope
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign(), page);
    }

    /**
     * 获取所有启用的域分类
     */
    @DataPermissionQuery
    @ApiOperation(value = "获取所有启用的域分类")
    @GetMapping("/enabled")
    public AjaxResult listEnabledDomains(PermissionQuery query) {
        List<DomainEntity> domains = service.listEnabledDomains(query);
        return AjaxResult.success(domains);
    }

    /**
     * 根据ID获取域分类详情
     */
    @ApiOperation(value = "根据ID获取域分类详情")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(service.findById(id));
    }

    /**
     * 新增域分类
     */
    @DataPermissionSave
    @ApiOperation(value = "新增域分类")
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DomainEntity domain) {
        // 检查名称是否已存在
        return toAjax(service.save(domain));
    }

    /**
     * 修改域分类
     */
    @DataPermissionSave
    @ApiOperation(value = "修改域分类")
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DomainEntity domain) {
        // 检查名称是否已存在（排除当前ID）
        service.update(domain);
        return AjaxResult.success();
    }

    /**
     * 删除域分类
     */
    @ApiOperation(value = "删除域分类")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        service.deleteByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 检查名称是否唯一
     */
    @DataPermissionQuery
    @ApiOperation(value = "检查名称是否唯一")
    @GetMapping("/checkName")
    public AjaxResult checkNameUnique(
                PermissionQuery query,
                @RequestParam String name,
                @RequestParam(required = false) Long excludeId) {
        boolean isUnique = !service.isNameExist(name, excludeId , query);
        return AjaxResult.success(isUnique);
    }

    @Override
    public IDomainService getFeign() {
        return this.service;
    }
}
package com.alinesno.infra.data.lake.storage.controller;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.lake.storage.entity.DownloadTokenEntity;
import com.alinesno.infra.data.lake.storage.entity.LakeCloudFileEntity;
import com.alinesno.infra.data.lake.storage.service.DownloadTokenService;
import com.alinesno.infra.data.lake.storage.service.ILakeCloudFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Token 管理 Controller（返回统一 AjaxResult）
 */
@Slf4j
@RestController
@RequestMapping("/api/lake/storage/storageToken")
public class StorageTokenController extends BaseController<DownloadTokenEntity, DownloadTokenService> {

    @Autowired
    private DownloadTokenService downloadTokenService;

    /**
     * 列表（分页）
     * GET /api/lake/storage/storageToken/list
     * params: pageNum, pageSize, keyword
     */
    @DataPermissionScope
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign(), page);
    }

    /**
     * 获取单个
     */
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        DownloadTokenEntity token = downloadTokenService.getById(id);
        if (token == null) return AjaxResult.error("未找到该 token");
        return AjaxResult.success("success", token);
    }

    /**
     * 创建
     */
    @DataPermissionSave
    @PostMapping
    public AjaxResult create(@RequestBody DownloadTokenEntity entity) {
        // 若未传 token，则生成一个随机 token
        if (entity.getToken() == null || entity.getToken().trim().isEmpty()) {
            entity.setToken(IdUtil.getSnowflakeNextIdStr());
        }
        // 设置默认值
        if (entity.getMaxUses() == null) entity.setMaxUses(0);
        if (entity.getUsedCount() == null) entity.setUsedCount(0);
        if (entity.getStatus() == null) entity.setStatus(0);
        // 使用 createTime 字段，确保实体中存在该字段（InfraBaseEntity 通常包含）
        entity.setAddTime(new Date());
        boolean ok = downloadTokenService.save(entity);
        if (!ok) return AjaxResult.error("创建失败");
        return AjaxResult.success("创建成功", entity);
    }

    /**
     * 更新
     */
    @PutMapping
    public AjaxResult update(@RequestBody DownloadTokenEntity entity) {
        if (entity.getId() == null) return AjaxResult.error("缺少 id");
        boolean ok = downloadTokenService.updateById(entity);
        if (!ok) return AjaxResult.error("更新失败");
        return AjaxResult.success("更新成功", entity);
    }

    /**
     * 删除（支持批量，逗号分隔）
     */
    @DeleteMapping("/{ids}")
    public AjaxResult delete(@PathVariable String ids) {
        String[] arr = ids.split(",");
        List<Long> idList = new ArrayList<>();
        for (String s : arr) {
            try {
                idList.add(Long.valueOf(s.trim()));
            } catch (NumberFormatException ignored) {}
        }
        if (idList.isEmpty()) return AjaxResult.error("无有效 id");
        boolean ok = downloadTokenService.removeByIds(idList);
        if (!ok) return AjaxResult.error("删除失败");
        return AjaxResult.success("删除成功");
    }

    /**
     * 生成随机 token 字符串（供前端直接调用）
     */
    @PostMapping("/generate")
    public AjaxResult generate() {
        String token = IdUtil.getSnowflakeNextIdStr();
        return AjaxResult.success("success", token);
    }

    @Override
    public DownloadTokenService getFeign() {
        return this.downloadTokenService;
    }
}
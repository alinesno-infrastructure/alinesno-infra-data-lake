package com.alinesno.infra.data.lake.storage.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.common.web.log.annotation.Log;
import com.alinesno.infra.common.web.log.enums.BusinessType;
import com.alinesno.infra.data.lake.storage.dto.CreateFolderDto;
import com.alinesno.infra.data.lake.storage.dto.FileStatisticsDto;
import com.alinesno.infra.data.lake.storage.dto.FileUploadDto;
import com.alinesno.infra.data.lake.storage.entity.LakeCloudFileEntity;
import com.alinesno.infra.data.lake.storage.service.ICatalogCloudFileService;
import com.alinesno.infra.data.lake.storage.service.ILakeCloudFileService;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 云文件控制器
 */
@Slf4j
@Api(tags = "CloudFile")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/lake/storage/cloudFile")
public class LakeCloudFileController extends BaseController<LakeCloudFileEntity, ILakeCloudFileService> {

    @Autowired
    private ILakeCloudFileService service;

    @Autowired
    private ICatalogCloudFileService catalogCloudFileService;

    /**
     * 获取文件列表的DataTables数据
     */
    @DataPermissionScope
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign(), page);
    }

    /**
     * 获取文件统计信息
     */
    @DataPermissionQuery
    @GetMapping("/statistics")
    public AjaxResult getFileStatistics(
            PermissionQuery permissionQuery,
            @RequestParam(value = "folderId", defaultValue = "0") Long folderId) {
        try {
            FileStatisticsDto statistics = service.getFileStatistics(folderId , permissionQuery);
            return AjaxResult.success("获取统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取文件统计信息失败", e);
            return AjaxResult.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 上传到目录存储中，会自动存储到域名根目录下
     */
    @SneakyThrows
    @PostMapping("/catalogUploadFile")
    public AjaxResult uploadToCatalogStorage(@RequestParam Long catalogId,
                                             @RequestParam("autoOverwrite") boolean autoOverwrite,
                                             @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
                                             @RequestParam("file") MultipartFile multipartFile) {
        catalogCloudFileService.uploadToCatalogStorage(catalogId, multipartFile , autoOverwrite , parentId);
        return AjaxResult.success() ;
    }

    /**
     * 创建文件夹
     */
    @DataPermissionSave
    @PostMapping("/folder")
    @Log(title = "创建文件夹", businessType = BusinessType.INSERT)
    public AjaxResult createFolder(@Valid @RequestBody CreateFolderDto dto) {
        LakeCloudFileEntity folder = service.createFolder(dto);
        return AjaxResult.success("文件夹创建成功", folder);
    }

    /**
     * 删除文件夹
     */
    @DeleteMapping("/deleteFolder")
    @Log(title = "删除文件夹", businessType = BusinessType.DELETE)
    public AjaxResult deleteFolder(Long id) {
        service.removeById(id);
        return AjaxResult.success("文件夹删除成功");
    }

    /**
     * 上传文件
     */
    @DataPermissionQuery
    @PostMapping("/upload")
    @Log(title = "上传文件", businessType = BusinessType.INSERT)
    public AjaxResult uploadFile(
            PermissionQuery permissionQuery,
            @RequestParam("file") MultipartFile file,
            @RequestParam("autoOverwrite") boolean autoOverwrite,
            @RequestParam(value = "parentId", defaultValue = "0") Long parentId
    ) {

        try {
            FileUploadDto dto = new FileUploadDto();
            BeanUtils.copyProperties(permissionQuery, dto);
            dto.setFile(file);
            dto.setParentId(parentId);
            dto.setAutoOverwrite(autoOverwrite);

            LakeCloudFileEntity fileEntity = service.uploadFile(dto);
            return AjaxResult.success("文件上传成功", fileEntity);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return AjaxResult.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文件
     */
    @PostMapping("/batchUpload")
    @Log(title = "批量上传文件", businessType = BusinessType.INSERT)
    public AjaxResult batchUploadFile(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "parentId", defaultValue = "0") Long parentId) {

        try {
            List<LakeCloudFileEntity> uploadedFiles = service.batchUploadFile(files, parentId);
            return AjaxResult.success("文件上传成功", uploadedFiles);
        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            return AjaxResult.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/deleteFile")
    @Log(title = "删除文件", businessType = BusinessType.DELETE)
    public AjaxResult deleteFile(@RequestParam Long id) {
        try {
            service.deleteFile(id);
            return AjaxResult.success("文件删除成功");
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return AjaxResult.error("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件夹树
     */
    @GetMapping("/folderTree")
    public AjaxResult getFolderTree() {
        try {
            List<LakeCloudFileEntity> folderTree = service.getFolderTree();
            return AjaxResult.success("获取成功", folderTree);
        } catch (Exception e) {
            log.error("获取文件夹树失败", e);
            return AjaxResult.error("获取文件夹树失败");
        }
    }

    @Override
    public ILakeCloudFileService getFeign() {
        return this.service;
    }
}
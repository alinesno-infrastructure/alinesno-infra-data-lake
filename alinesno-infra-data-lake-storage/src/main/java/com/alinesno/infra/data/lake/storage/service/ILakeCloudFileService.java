package com.alinesno.infra.data.lake.storage.service;

import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.lake.storage.dto.CreateFolderDto;
import com.alinesno.infra.data.lake.storage.dto.FileStatisticsDto;
import com.alinesno.infra.data.lake.storage.dto.FileUploadDto;
import com.alinesno.infra.data.lake.storage.entity.LakeCloudFileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 云文件服务接口
 */
public interface ILakeCloudFileService extends IBaseService<LakeCloudFileEntity> {

    /**
     * 文件夹是否存在
     */
    boolean folderExists(CreateFolderDto dto);

    /**
     * 创建文件夹
     */
    LakeCloudFileEntity createFolder(CreateFolderDto dto);

    /**
     * 上传文件
     */
    LakeCloudFileEntity uploadFile(FileUploadDto dto);

    /**
     * 批量上传文件
     */
    List<LakeCloudFileEntity> batchUploadFile(MultipartFile[] files, Long parentId);

    /**
     * 删除文件
     */
    void deleteFile(Long id);

    /**
     * 获取文件夹树
     */
    List<LakeCloudFileEntity> getFolderTree();

    /**
     * 文件安全验证
     * @param file
     */
    void validateFileSecurity(MultipartFile file);

    /**
     * 获取文件统计信息
     *
     * @param folderId        文件夹ID（0表示根目录）
     * @param permissionQuery
     * @return 文件统计信息
     */
    FileStatisticsDto getFileStatistics(Long folderId, PermissionQuery permissionQuery);

    /**
     * 根据名称获取文件夹
     * @param createFolderDto
     * @return
     */
    LakeCloudFileEntity getFolderByName(CreateFolderDto createFolderDto);
}
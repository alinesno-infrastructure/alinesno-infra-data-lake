package com.alinesno.infra.data.lake.storage.service;

import com.alinesno.infra.data.lake.storage.entity.LakeCloudFileEntity;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface ILakeCloudDownloadFileService {
    LakeCloudFileEntity getById(Long id);

    /**
     * 返回文件夹下的所有文件（可递归）
     */
    List<LakeCloudFileEntity> listFilesUnderFolder(Long folderId);

    /**
     * 返回文件夹下，位于时间区间 [startTime, endTime] 的文件（含端点）。若 startTime 或 endTime 为 null，则视为不限制。
     * 仅返回 isDirectory == 0 的文件。
     */
    List<LakeCloudFileEntity> listFilesUnderFolderByDate(Long folderId, Date startTime, Date endTime);
}
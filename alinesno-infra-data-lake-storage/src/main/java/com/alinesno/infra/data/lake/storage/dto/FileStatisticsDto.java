package com.alinesno.infra.data.lake.storage.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件统计DTO
 */
@Data
public class FileStatisticsDto implements Serializable {
    
    /**
     * 总文件大小（带单位的格式化字符串）
     */
    private String totalSize;
    
    /**
     * 总文件数量（非文件夹）
     */
    private Long totalFiles;
    
    /**
     * 今日上传文件数量
     */
    private Long todayUploads;
    
    /**
     * 本周上传文件数量
     */
    private Long weekUploads;
    
    /**
     * 统计的文件夹ID（0表示根目录）
     */
    private Long folderId;
    
    /**
     * 文件夹名称
     */
    private String folderName;
}
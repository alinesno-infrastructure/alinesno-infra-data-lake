package com.alinesno.infra.data.lake.api;

import lombok.Data;

import java.util.List;

/**
 * 接口响应DTO
 */
@Data
public class IngestionResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 插入的记录数
     */
    private Integer insertedCount;
    
    /**
     * 失败记录数
     */
    private Integer failedCount;
    
    /**
     * 失败详情
     */
    private List<String> failureDetails;

    private Long processedTime; // 处理耗时(ms)
    private String fileSize;    // 文件大小
    private Integer totalRows;  // 总行数

    public IngestionResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public IngestionResponse(boolean success, String message, Integer insertedCount) {
        this.success = success;
        this.message = message;
        this.insertedCount = insertedCount;
    }
}
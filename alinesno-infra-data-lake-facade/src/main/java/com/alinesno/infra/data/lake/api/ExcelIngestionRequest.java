package com.alinesno.infra.data.lake.api;

import lombok.Data;

/**
 * Excel数据入湖请求DTO
 */
@Data
public class ExcelIngestionRequest {
    
    /**
     * 目录ID
     */
    private Long catalogId;
    
    /**
     * Schema名称
     */
    private String schemaName;
    
    /**
     * 表名称
     */
    private String tableName;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 组织ID
     */
    private Long orgId;
    
//    /**
//     * Excel文件Base64编码（可选，如果通过文件上传方式）
//     */
//    private String excelBase64;

    private String fileName ;

    private long fileSize ;

}
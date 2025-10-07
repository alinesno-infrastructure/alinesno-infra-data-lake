package com.alinesno.infra.data.lake.api;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据入湖请求DTO
 */
@Data
public class DataLakeIngestionRequest {
    
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
     * 数据列表，每个Map对应一行数据，key为字段名，value为字段值
     */
    private List<Map<String, Object>> data;

}



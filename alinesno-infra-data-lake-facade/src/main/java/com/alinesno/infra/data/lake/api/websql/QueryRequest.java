package com.alinesno.infra.data.lake.api.websql;

import lombok.Data;
import java.util.Map;

/**
 * SQL查询请求DTO
 */
@Data
public class QueryRequest {
    /** 待执行的SQL语句 */
    private String sql;
    /** 页码（前端传递，后端可用于校验） */
    private Integer page;
    /** 每页条数（前端传递，后端可用于校验） */
    private Integer pageSize;
    /** 表上下文信息（catalogName/schemaName/tableName等） */
    private Map<String, Object> tableContext;
}
package com.alinesno.infra.data.lake.api.websql;

import lombok.Data;
import java.util.Map;

/**
 * SQL Explain请求DTO
 */
@Data
public class ExplainRequest {
    /** 待解析的SQL语句 */
    private String sql;
    /** 表上下文信息 */
    private Map<String, Object> tableContext;
}
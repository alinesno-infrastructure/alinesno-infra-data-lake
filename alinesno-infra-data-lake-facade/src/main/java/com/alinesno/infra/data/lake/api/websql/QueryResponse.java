package com.alinesno.infra.data.lake.api.websql;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * SQL查询响应DTO（适配前端格式）
 */
@Data
public class QueryResponse {
    /** 列名列表（支持字符串或{key, title}格式，这里简化为字符串列表） */
    private List<String> columns;
    /** 数据行列表（Map结构，key为列名，value为字段值） */
    private List<Map<String, Object>> rows;
    /** 总记录数（-1表示未知） */
    private Long total;
    /** 错误信息（成功时为null） */
    private String error;
}
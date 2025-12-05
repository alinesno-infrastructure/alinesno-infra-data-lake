package com.alinesno.infra.data.lake.service;

import com.alinesno.infra.data.lake.api.websql.QueryRequest;
import com.alinesno.infra.data.lake.api.websql.QueryResponse;

/**
 * Iceberg SQL查询服务接口
 */
public interface IQueryService {

    /**
     * 执行SQL查询
     * @param request 查询请求
     * @return 查询结果（含列、数据、总数）
     */
    QueryResponse executeQuery(QueryRequest request);

    /**
     * 解析SQL执行计划
     * @param sql 待解析SQL
     * @param tableContext 表上下文
     * @return 执行计划字符串
     */
    String explainSql(String sql, Object tableContext);
}
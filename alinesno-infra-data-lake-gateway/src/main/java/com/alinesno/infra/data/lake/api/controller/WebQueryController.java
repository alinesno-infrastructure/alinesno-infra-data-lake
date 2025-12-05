package com.alinesno.infra.data.lake.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.data.lake.api.websql.ExplainRequest;
import com.alinesno.infra.data.lake.api.websql.QueryRequest;
import com.alinesno.infra.data.lake.api.websql.QueryResponse;
import com.alinesno.infra.data.lake.service.IQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Iceberg SQL查询控制器（适配前端接口）
 */
@Slf4j
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/lake/webQuery")
public class WebQueryController {

    @Autowired
    private IQueryService queryService;

    /**
     * 执行SQL查询（前端调用接口）
     */
    @PostMapping
    public QueryResponse query(@RequestBody QueryRequest request) {
        log.info("接收SQL查询请求：{}", request.getSql());
        return queryService.executeQuery(request);
    }

    /**
     * 解析SQL执行计划（前端调用接口）
     */
    @PostMapping("/explain")
    public String explain(@RequestBody ExplainRequest request) {
        log.info("接收SQL Explain请求：{}", request.getSql());
        return queryService.explainSql(request.getSql(), request.getTableContext());
    }
}
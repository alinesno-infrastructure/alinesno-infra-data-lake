package com.alinesno.infra.data.lake.storage.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * InsertDataDto 类
 */
@ToString
@Data
public class InsertDataDto {

    private String tableId ; // 表ID

    private List<Map<String , Object>> dataMap ; // 数据

}

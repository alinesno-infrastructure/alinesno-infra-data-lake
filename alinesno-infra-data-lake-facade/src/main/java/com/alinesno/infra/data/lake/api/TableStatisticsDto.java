package com.alinesno.infra.data.lake.api;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TableStatisticsDto {
    // 基础信息
    private String location; // 存储位置
    private Long fileCount; // 文件数量
    private Long totalSize; // 总大小（字节）
    private Long recordCount; // 记录数
    private String lastUpdated; // 最后更新时间
    private String formatVersion; // 格式版本
    private String currentSnapshotId; // 当前快照ID
    private String tableType; // 表类型
    private String owner; // 表所有者
    private String createdTime; // 创建时间

    // 从DDL中提取的属性
    private String formatType; // 存储格式（PARQUET, ORC等）
    private String partitioning; // 分区信息
    private String sortOrder; // 排序信息
    private String maxCommitRetry; // 最大提交重试次数

    // 其他Iceberg属性
    private Map<String, String> properties = new HashMap<>();
}
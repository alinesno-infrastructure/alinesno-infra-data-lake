package com.alinesno.infra.data.lake.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据湖目录层级枚举
 */
@Getter
@AllArgsConstructor
public enum DirectoryLevel {

    GLOBAL("global", "全局层，存放全局共享的数据"),
    RAW("raw", "原始层，存放未经处理的原始数据"),
    ENRICHED("enriched", "清洗层，存放经过清洗和转换的数据"),
    CURATED("curated", "治理层，存放经过治理和标准化的数据"),
    SANDBOX("sandbox", "沙箱区，用于数据探索和实验"),
    SYSTEM("system", "系统层，存放系统相关的数据"),
    SEMANTIC("semantic", "语义层，存放面向业务的数据模型");

    private final String name;
    private final String description;

}
package com.alinesno.infra.data.lake.api.iceberg;

import lombok.Data;

/**
 * 湖数据统计数据传输对象
 */
@Data
public class LakeCountStatsDto {

    private String totalStorageSize; // 获取整个Catalog的总存储量
    private long schemeCount ; // 获取Catalog下的所有Schema数量
    private long schemeTableCount; // 获取Catalog下的所有Schema下的所有表的数量
    private String formattedSize ;  // 系统总存储量


}

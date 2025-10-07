package com.alinesno.infra.data.lake.api.iceberg;

import lombok.Data;

/**
 * 表基本信息
 */
@Data
public class TableInfoDto {
    private String tableName;
    private String databaseName;
    private String location;
    private long sizeBytes;
    private boolean partitioned;
    private long lastUpdated;
    
    // 格式化方法
    public String getFormattedSize() {
        return formatBytes(sizeBytes);
    }
    
    public String getFormattedLastUpdated() {
        return new java.util.Date(lastUpdated).toString();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
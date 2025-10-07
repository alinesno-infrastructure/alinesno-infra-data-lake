package com.alinesno.infra.data.lake.api.iceberg;

import lombok.Data;

/**
 * 数据库存储统计信息
 */
@Data
public class DatabaseStorageStats {
    private String databaseName;
    private long totalSize;        // 总大小（字节）
    private long fileCount;        // 文件数量
    private long recordCount;      // 记录数
    private int tableCount;        // 表数量
    
    // 格式化方法
    public String getFormattedSize() {
        return formatBytes(totalSize);
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
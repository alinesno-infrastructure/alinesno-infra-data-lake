package com.alinesno.infra.data.lake.api.iceberg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表存储统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableStorageStats {
    private String databaseName;
    private String tableName;
    private long totalSizeBytes;    // 总大小（字节）
    private int fileCount;          // 文件数量
    private long snapshotSizeBytes; // 快照大小（字节）
    
    // 格式化方法
    public String getFormattedSize() {
        return formatBytes(totalSizeBytes);
    }
    
    public String getFormattedSnapshotSize() {
        return formatBytes(snapshotSizeBytes);
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
# 数据库表查询 

```sql
-- Iceberg命名空间属性表
CREATE TABLE iceberg_namespace_properties (
    catalog_name VARCHAR(255) NOT NULL COMMENT 'Iceberg目录名称',
    namespace VARCHAR(255) NOT NULL COMMENT '命名空间名称',
    property_key VARCHAR(255) NOT NULL COMMENT '属性键名称',
    property_value VARCHAR(5000) COMMENT '属性键对应的值',
    PRIMARY KEY (catalog_name, namespace, property_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='存储Iceberg命名空间的属性配置信息';

-- Iceberg表元数据表
CREATE TABLE iceberg_tables (
    catalog_name VARCHAR(255) NOT NULL COMMENT 'Iceberg目录名称',
    table_namespace VARCHAR(255) NOT NULL COMMENT '表所在的命名空间',
    table_name VARCHAR(255) NOT NULL COMMENT '表名称',
    metadata_location VARCHAR(5000) NOT NULL COMMENT '当前元数据文件位置',
    previous_metadata_location VARCHAR(5000) COMMENT '前一个元数据文件位置',
    PRIMARY KEY (catalog_name, table_namespace, table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='存储Iceberg表的元数据位置信息';

-- 为iceberg_namespace_properties表添加索引
CREATE INDEX idx_namespace_properties_ns ON iceberg_namespace_properties (catalog_name, namespace);
CREATE INDEX idx_namespace_properties_key ON iceberg_namespace_properties (property_key(100));

-- 为iceberg_tables表添加索引
CREATE INDEX idx_iceberg_tables_ns ON iceberg_tables (catalog_name, table_namespace);
CREATE INDEX idx_iceberg_tables_name ON iceberg_tables (table_name);
CREATE INDEX idx_iceberg_tables_location ON iceberg_tables (metadata_location(100));
```
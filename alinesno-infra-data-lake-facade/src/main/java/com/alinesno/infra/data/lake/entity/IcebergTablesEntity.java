package com.alinesno.infra.data.lake.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Iceberg 表实体
 * 对应表: iceberg_tables
 * 主键为复合主键 (catalog_name, table_namespace, table_name)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("iceberg_tables")
@Table(value = "iceberg_tables", comment = "iceberg-表信息表")
public class IcebergTablesEntity {

    @TableField(value = "catalog_name", fill = FieldFill.DEFAULT)
    @Column(name = "catalog_name", type = MySqlTypeConstant.VARCHAR, comment = "Catalog 名称")
    private String catalogName;

    @TableField(value = "table_namespace", fill = FieldFill.DEFAULT)
    @Column(name = "table_namespace", type = MySqlTypeConstant.VARCHAR, comment = "表命名空间")
    private String tableNamespace;

    @TableField(value = "table_name", fill = FieldFill.DEFAULT)
    @Column(name = "table_name", type = MySqlTypeConstant.VARCHAR, comment = "表名")
    private String tableName;

    @TableField("metadata_location")
    @Column(name = "metadata_location", type = MySqlTypeConstant.VARCHAR, length = 1000, comment = "元数据位置")
    private String metadataLocation;

    @TableField("previous_metadata_location")
    @Column(name = "previous_metadata_location", type = MySqlTypeConstant.VARCHAR, length = 1000, comment = "上一次元数据位置")
    private String previousMetadataLocation;

    // iceberg_type
    @TableField("iceberg_type")
    @Column(name = "iceberg_type", type = MySqlTypeConstant.VARCHAR, length = 1000, comment = "Iceberg 类型")
    private String icebergType;

}
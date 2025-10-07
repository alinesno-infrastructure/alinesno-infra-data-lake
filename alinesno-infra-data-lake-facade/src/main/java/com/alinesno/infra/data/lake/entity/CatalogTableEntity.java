package com.alinesno.infra.data.lake.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据湖数据表实体类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("dl_catalog_table")
@Table(name = "dl_catalog_table")
public class CatalogTableEntity extends InfraBaseEntity {

    @TableField
    @Column(name = "table_name", type = MySqlTypeConstant.VARCHAR, length = 100 , comment = "数据表名称")
    private String tableName;

    @TableField
    @Column(name = "catalog_id", type = MySqlTypeConstant.BIGINT , comment = "目录ID")
    private Long catalogId;

    @TableField
    @Column(name = "description", type = MySqlTypeConstant.VARCHAR, length = 500 , comment = "描述")
    private String description;

    @TableField
    @Column(name = "storage_size", type = MySqlTypeConstant.DOUBLE , comment = "存储大小")
    private Double storageSize;

    @TableField
    @Column(name = "storage_unit", type = MySqlTypeConstant.VARCHAR, length = 10 , comment = "存储大小单位")
    private String storageUnit;

    @TableField
    @Column(name = "row_count", type = MySqlTypeConstant.BIGINT , comment = "行数")
    private Long rowCount;

    @TableField
    @Column(name = "format_type", type = MySqlTypeConstant.VARCHAR, length = 20 , comment = "格式类型")
    private String formatType; // Parquet, ORC, CSV等

    @TableField
    @Column(name = "storage_location", type = MySqlTypeConstant.VARCHAR, length = 500 , comment = "存储位置")
    private String storageLocation;

    @TableField
    @Column(name = "parent_id", type = MySqlTypeConstant.BIGINT , comment = "父级ID")
    private Long parentId ;

    @TableField
    @Column(name = "is_directory", type = MySqlTypeConstant.SMALLINT , comment = "是否为目录")
    @JsonProperty("isDirectory") // 添加这个注解
    private boolean isDirectory;

    @TableField
    @Column(name = "scheme_name", type = MySqlTypeConstant.VARCHAR, length = 100 , comment = "数据库scheme")
    private String schemeName ;

}
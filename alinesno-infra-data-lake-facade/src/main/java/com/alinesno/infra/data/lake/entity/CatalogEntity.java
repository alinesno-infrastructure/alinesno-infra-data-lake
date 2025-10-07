package com.alinesno.infra.data.lake.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 数据目录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("dl_catalog")
@Table(name = "dl_catalog")
public class CatalogEntity extends InfraBaseEntity {

    @TableField
    @Column(name = "catalog_name", type = MySqlTypeConstant.VARCHAR, length = 100 , comment = "目录名称")
    private String catalogName;

    @TableField
    @Column(name = "description", type = MySqlTypeConstant.VARCHAR, length = 500 , comment = "描述")
    private String description;

    @TableField
    @Column(name = "icon", type = MySqlTypeConstant.VARCHAR, length = 50 , comment = "图标")
    private String icon;

    @TableField
    @Column(name = "table_count", type = MySqlTypeConstant.INT , comment = "数据表数量")
    private Integer tableCount;

    @TableField
    @Column(name = "storage_size", type = MySqlTypeConstant.DOUBLE , comment = "存储大小")
    private Double storageSize;

    @TableField
    @Column(name = "storage_unit", type = MySqlTypeConstant.VARCHAR, length = 10 , comment = "存储大小单位")
    private String storageUnit;

    @TableField
    @Column(name = "status", type = MySqlTypeConstant.VARCHAR, length = 20 , comment = "状态")
    private String status; // 活跃/归档

    @TableField
    @Column(name = "owner_id", type = MySqlTypeConstant.BIGINT , comment = "所有者ID")
    private Long ownerId;

    @TableField
    @Column(name = "scheme_name", type = MySqlTypeConstant.VARCHAR, length = 100 , comment = "Catalog目录下面的scheme名称")
    private String schemeName;

    // 关联的数据表列表（非数据库字段）
    @TableField(exist = false)
    private List<CatalogTableEntity> tables;

}
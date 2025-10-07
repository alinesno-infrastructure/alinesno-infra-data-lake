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
 * Iceberg 命名空间属性实体
 * 对应表: iceberg_namespace_properties
 * 主键为复合主键 (catalog_name, namespace, property_key)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("iceberg_namespace_properties")
@Table(value = "iceberg_namespace_properties", comment = "iceberg-命名空间属性表")
public class IcebergNamespacePropertiesEntity {

    @TableField(value = "catalog_name", fill = FieldFill.DEFAULT)
    @Column(name = "catalog_name", type = MySqlTypeConstant.VARCHAR,  comment = "Catalog 名称")
    private String catalogName;

    @TableField(value = "namespace", fill = FieldFill.DEFAULT)
    @Column(name = "namespace", type = MySqlTypeConstant.VARCHAR,  comment = "命名空间")
    private String namespace;

    @TableField(value = "property_key", fill = FieldFill.DEFAULT)
    @Column(name = "property_key", type = MySqlTypeConstant.VARCHAR,  comment = "属性键")
    private String propertyKey;

    @TableField(value = "property_value")
    @Column(name = "property_value", type = MySqlTypeConstant.VARCHAR, length = 1000, comment = "属性值")
    private String propertyValue;

}
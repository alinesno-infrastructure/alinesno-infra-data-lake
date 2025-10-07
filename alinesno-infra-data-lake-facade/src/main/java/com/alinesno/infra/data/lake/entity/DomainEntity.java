package com.alinesno.infra.data.lake.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
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
 * 数据湖域分类实体
 * 代表数据湖中一个逻辑上的数据域或主题域
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("dl_domain") // MyBatis-Plus 表名
@Table(value = "dl_domain", comment = "数据湖-域分类表") // A-Table 表名及注释
public class DomainEntity extends InfraBaseEntity {

    @TableField(value = "icon", fill = FieldFill.DEFAULT)
    @Column(name = "icon", type = MySqlTypeConstant.VARCHAR, length = 100, comment = "图标")
    private String icon ;

    @TableField(value = "name", fill = FieldFill.DEFAULT)
    @Column(name = "name", type = MySqlTypeConstant.VARCHAR, length = 100, isNull = false, comment = "域分类名称")
    private String name;

    @TableField("sort_order")
    @Column(name = "sort_order", type = MySqlTypeConstant.INT, defaultValue = "1", comment = "排序序号")
    private Integer sortOrder;

    @TableField("description")
    @Column(name = "description", type = MySqlTypeConstant.VARCHAR, length = 255, comment = "域分类描述")
    private String description;

}
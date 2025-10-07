package com.alinesno.infra.data.lake.mapper;

import com.alinesno.infra.common.facade.mapper.repository.IBaseMapper;
import com.alinesno.infra.data.lake.entity.DomainEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据湖域分类 Mapper 接口
 */
@Mapper
public interface DomainMapper extends IBaseMapper<DomainEntity> {

    // 可以在此添加自定义的复杂查询方法
    // 例如：根据状态查询域分类列表
    // List<Domain> selectByStatus(@Param("status") Integer status);
}
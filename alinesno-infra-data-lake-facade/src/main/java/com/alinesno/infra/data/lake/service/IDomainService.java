package com.alinesno.infra.data.lake.service;

import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.lake.entity.DomainEntity;

import java.util.List;

/**
 * 数据湖域分类服务接口
 */
public interface IDomainService extends IBaseService<DomainEntity> {

    /**
     * 获取所有启用的域分类（按排序序号升序）
     * @return 启用的域分类列表
     */
    List<DomainEntity> listEnabledDomains(PermissionQuery query);

    /**
     * 根据名称查找域分类
     * @param name 域分类名称
     * @return 匹配的域分类
     */
    DomainEntity getByName(String name);

    /**
     * 检查域分类名称是否已存在
     *
     * @param name      域分类名称
     * @param excludeId 排除的ID（用于更新操作）
     * @param query
     * @return 是否存在
     */
    boolean isNameExist(String name, Long excludeId, PermissionQuery query);
}
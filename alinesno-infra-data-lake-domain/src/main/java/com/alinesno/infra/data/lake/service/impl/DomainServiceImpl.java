package com.alinesno.infra.data.lake.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.enums.HasStatusEnums;
import com.alinesno.infra.data.lake.entity.DomainEntity;
import com.alinesno.infra.data.lake.mapper.DomainMapper;
import com.alinesno.infra.data.lake.service.IDomainService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 数据湖域分类服务实现
 */
@Slf4j
@Service
public class DomainServiceImpl extends IBaseServiceImpl<DomainEntity, DomainMapper> implements IDomainService {

    @Override
    public List<DomainEntity> listEnabledDomains(PermissionQuery query) {
        LambdaQueryWrapper<DomainEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DomainEntity::getHasStatus, HasStatusEnums.LEGAL.value) // 状态为启用
                .eq(DomainEntity::getOrgId, query.getOrgId())
                .orderByAsc(DomainEntity::getSortOrder); // 按排序序号升序

        return this.list(queryWrapper);
    }

    @Override
    public DomainEntity getByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        
        LambdaQueryWrapper<DomainEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DomainEntity::getName, name)
                   .last("LIMIT 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean isNameExist(String name, Long excludeId, PermissionQuery query) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        
        LambdaQueryWrapper<DomainEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DomainEntity::getName, name) ;

        if (excludeId != null) {
            queryWrapper.ne(DomainEntity::getId, excludeId);
        }

        long count = this.count(queryWrapper) ;
        log.debug("count = {}", count);
        
        return count > 0;
    }
}
package com.alinesno.infra.data.lake.mapper;

import com.alinesno.infra.common.facade.mapper.repository.IBaseMapper;
import com.alinesno.infra.data.lake.entity.StorageFileEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StorageFileMapper extends IBaseMapper<StorageFileEntity> {
}

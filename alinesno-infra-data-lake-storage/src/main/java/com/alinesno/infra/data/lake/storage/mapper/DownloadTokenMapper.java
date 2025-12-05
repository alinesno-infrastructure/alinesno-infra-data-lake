package com.alinesno.infra.data.lake.storage.mapper;

import com.alinesno.infra.common.facade.mapper.repository.IBaseMapper;
import com.alinesno.infra.data.lake.storage.entity.DownloadTokenEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DownloadTokenMapper extends IBaseMapper<DownloadTokenEntity> {
}


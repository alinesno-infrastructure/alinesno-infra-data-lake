package com.alinesno.infra.data.lake.storage.mapper;

import com.alinesno.infra.data.lake.storage.entity.DownloadRecordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DownloadRecordMapper extends BaseMapper<DownloadRecordEntity> {
}
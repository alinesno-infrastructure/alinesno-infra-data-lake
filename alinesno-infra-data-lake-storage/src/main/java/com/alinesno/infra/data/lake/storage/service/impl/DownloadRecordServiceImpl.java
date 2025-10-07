package com.alinesno.infra.data.lake.storage.service.impl;

import com.alinesno.infra.data.lake.storage.entity.DownloadRecordEntity;
import com.alinesno.infra.data.lake.storage.mapper.DownloadRecordMapper;
import com.alinesno.infra.data.lake.storage.service.DownloadRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class DownloadRecordServiceImpl extends ServiceImpl<DownloadRecordMapper, DownloadRecordEntity>
        implements DownloadRecordService {
}
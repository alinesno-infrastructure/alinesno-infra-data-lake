package com.alinesno.infra.data.lake.service.impl;

import com.alinesno.infra.data.lake.api.DataLakeIngestionRequest;
import com.alinesno.infra.data.lake.api.ExcelIngestionRequest;
import com.alinesno.infra.data.lake.api.IngestionResponse;
import com.alinesno.infra.data.lake.service.IDataLakeIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 数据入湖服务实现
 */
@Slf4j
@Service
public class DataLakeIngestionServiceImpl implements IDataLakeIngestionService {
    @Override
    public IngestionResponse ingestData(DataLakeIngestionRequest request) {
        return null;
    }

    @Override
    public IngestionResponse ingestExcelDataStream(ExcelIngestionRequest request, InputStream inputStream) {
        return null;
    }

}
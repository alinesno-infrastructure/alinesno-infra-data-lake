package com.alinesno.infra.data.lake.service;

import com.alinesno.infra.data.lake.api.DataLakeIngestionRequest;
import com.alinesno.infra.data.lake.api.ExcelIngestionRequest;
import com.alinesno.infra.data.lake.api.IngestionResponse;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

public interface IDataLakeIngestionService {

    @Transactional
    IngestionResponse ingestData(DataLakeIngestionRequest request);

    /**
     * 批量插入数据
     * @param request
     * @param inputStream
     * @return
     */
    IngestionResponse ingestExcelDataStream(ExcelIngestionRequest request, InputStream inputStream);
}

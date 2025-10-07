package com.alinesno.infra.data.lake.api.provider;

import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.data.lake.api.DataLakeIngestionRequest;
import com.alinesno.infra.data.lake.api.ExcelIngestionRequest;
import com.alinesno.infra.data.lake.api.IngestionResponse;
import com.alinesno.infra.data.lake.service.IDataLakeIngestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 数据入湖接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/lake/ingestion")
@Api(tags = "数据入湖接口")
public class DataLakeIngestionController {

    @Autowired
    private IDataLakeIngestionService ingestionService;
    
    /**
     * 数据入湖接口
     */
    @ApiOperation("数据入湖接口")
    @PostMapping("/ingest")
    public AjaxResult ingestData(@RequestBody DataLakeIngestionRequest request) {
        log.info("接收到数据入湖请求: catalogId={}, schema={}, table={}, 记录数={}", 
                request.getCatalogId(), request.getSchemaName(), 
                request.getTableName(), request.getData().size());
        
        IngestionResponse response = ingestionService.ingestData(request);
        
        if (response.isSuccess()) {
            return AjaxResult.success(response.getMessage() , response.getInsertedCount()) ;
        } else {
            return AjaxResult.error(response.getMessage());
        }
    }

    /**
     * Excel数据入湖接口（通过文件上传）- 流式处理优化版
     */
    @ApiOperation("Excel数据入湖接口(文件上传)")
    @PostMapping("/ingest/excel/upload")
    public AjaxResult ingestExcelDataUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long catalogId,
            @RequestParam String schemaName,
            @RequestParam String tableName) {

        log.info("接收到Excel文件上传入湖请求: catalogId={}, schema={}, table={}, 文件名={}, 文件大小={}字节",
                catalogId, schemaName, tableName, file.getOriginalFilename(), file.getSize());

        // 文件大小验证
        if (file.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }

        if (file.getSize() > 100 * 1024 * 1024) { // 100MB限制
            return AjaxResult.error("文件大小不能超过100MB");
        }

        try (InputStream inputStream = file.getInputStream()) {

            ExcelIngestionRequest request = new ExcelIngestionRequest();
            request.setCatalogId(catalogId);
            request.setSchemaName(schemaName);
            request.setTableName(tableName);
            request.setFileName(file.getOriginalFilename());
            request.setFileSize(file.getSize());

            // 流式处理Excel文件
            IngestionResponse response = ingestionService.ingestExcelDataStream(request, inputStream);

            if (response.isSuccess()) {
                AjaxResult result = AjaxResult.success(response.getMessage()) ;
                result.put("insertedCount", response.getInsertedCount());
                result.put("processedTime", response.getProcessedTime());

                return result ;
            } else {
                AjaxResult result = AjaxResult.error(response.getMessage());
                result.put("failedCount", response.getFailedCount());

                return result ;
            }

        } catch (IOException e) {
            log.error("处理Excel文件失败", e);
            return AjaxResult.error("处理Excel文件失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Excel数据入湖处理异常", e);
            return AjaxResult.error("数据处理异常: " + e.getMessage());
        }
    }

}
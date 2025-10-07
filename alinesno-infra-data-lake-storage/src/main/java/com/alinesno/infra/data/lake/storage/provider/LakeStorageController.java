package com.alinesno.infra.data.lake.storage.provider;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.lake.storage.dto.InsertDataDto;
import com.alinesno.infra.data.lake.storage.service.ICatalogCloudFileService;
import com.alinesno.infra.data.lake.storage.utils.ExcelParserUtils;
import io.swagger.annotations.Api;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 湖数据ODS层数据存储控制器
 */
@Slf4j
@Api(tags = "LakeCloudFile")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/lake/storage")
public class LakeStorageController {

    @Autowired
    private ICatalogCloudFileService catalogCloudFileService;

    /**
     * 上传到目录存储中，会自动存储到域名根目录下
     */
    @SneakyThrows
    @PutMapping("/uploadToCatalog")
    public R<String> uploadToCatalogStorage(@RequestParam Long catalogId,
                                            @RequestParam("autoOverwrite") boolean autoOverwrite,
                                            @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
                                            @RequestParam("file") MultipartFile multipartFile) {
        catalogCloudFileService.uploadToCatalogStorage(catalogId, multipartFile, autoOverwrite, parentId);
        return R.ok() ;
    }

    /**
     * JSON数据存入Iceberg
     */
    @PutMapping("/insert")
    public R<String> insertIcebergData(@RequestBody InsertDataDto jsonData) {

        log.debug("接收到的JSON数据：{}", jsonData);

        // 调用服务层方法处理数据插入
        catalogCloudFileService.insertJsonDataToIceberg(jsonData);
        return R.ok("数据成功插入Iceberg表");
    }

    /**
     * Excel文件上传并解析数据存入Iceberg
     */
    @SneakyThrows
    @PostMapping("/uploadExcel")
    public R<String> uploadExcelFile(@RequestParam Long tableId,
                                     @RequestParam("file") MultipartFile multipartFile) {

        log.info("开始处理Excel文件上传，表ID: {}", tableId);
        log.info("文件名: {}, 文件大小: {} bytes", multipartFile.getOriginalFilename(), multipartFile.getSize());

        // 使用工具类解析Excel文件
        InsertDataDto insertDataDto = ExcelParserUtils.parseExcelFile(multipartFile, tableId);

        log.info("Excel文件解析完成，共解析 {} 行数据", insertDataDto.getDataMap().size());

        // 调用服务层方法处理数据插入
        catalogCloudFileService.insertJsonDataToIceberg(insertDataDto);

        return R.ok("Excel文件数据成功插入Iceberg表");
    }

}

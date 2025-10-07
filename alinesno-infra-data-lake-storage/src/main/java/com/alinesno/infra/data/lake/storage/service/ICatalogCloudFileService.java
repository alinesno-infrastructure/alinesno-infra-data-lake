package com.alinesno.infra.data.lake.storage.service;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.data.lake.adapter.service.CloudStorageConsumer;
import com.alinesno.infra.data.lake.entity.CatalogEntity;
import com.alinesno.infra.data.lake.entity.CatalogTableEntity;
import com.alinesno.infra.data.lake.handle.IcebergTableUtils;
import com.alinesno.infra.data.lake.service.ICatalogService;
import com.alinesno.infra.data.lake.service.ICatalogTableService;
import com.alinesno.infra.data.lake.storage.dto.CreateFolderDto;
import com.alinesno.infra.data.lake.storage.dto.FileUploadDto;
import com.alinesno.infra.data.lake.storage.dto.InsertDataDto;
import com.alinesno.infra.data.lake.storage.entity.LakeCloudFileEntity;
import com.alinesno.infra.data.lake.storage.utils.IcebergConvertUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.*;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.GenericParquetWriter;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.types.Types;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.lang.exception.RpcServiceRuntimeException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 目录云文件服务接口
 */
@Slf4j
@Component
public class ICatalogCloudFileService {

    @Autowired
    private ILakeCloudFileService lakeCloudFileService;

    @Autowired
    private ICatalogService catalogService;

    @Autowired
    private CloudStorageConsumer cloudStorageConsumer;

    @Autowired
    private ICatalogTableService catalogTableService ;

    @Autowired
    private IcebergTableUtils icebergTableUtils;

    /**
     * 上传到目录存储中，会自动存储到域名根目录下
     */
    public void uploadToCatalogStorage(Long catalogId,
                                       MultipartFile multipartFile,
                                       boolean autoOverwrite,
                                       Long parentId) {

        // 获取目录信息
        CatalogEntity catalogEntity = catalogService.getById(catalogId);
        if (catalogEntity == null) {
            throw new RpcServiceRuntimeException("目录不存在");
        }

        // 文件上传云存储
        String folderName = "业务域:" + catalogEntity.getCatalogName() ;

        CreateFolderDto createFolderDto = new CreateFolderDto();
        BeanUtils.copyProperties(catalogEntity, createFolderDto);
        createFolderDto.setParentId(0L) ;
        createFolderDto.setFolderName(folderName);

        // Long parentId;

        LakeCloudFileEntity folder;

        if(!lakeCloudFileService.folderExists(createFolderDto)){  // 不存在，则创建
            folder = lakeCloudFileService.createFolder(createFolderDto);
            parentId = folder.getId() ;
        }else{
            if(parentId == 0){
                folder = lakeCloudFileService.getFolderByName(createFolderDto);
                parentId = folder.getId() ;
            }
        }

        FileUploadDto fileUploadDto = new FileUploadDto();
        BeanUtils.copyProperties(catalogEntity, fileUploadDto);

        fileUploadDto.setFile(multipartFile);
        fileUploadDto.setParentId(parentId);
        fileUploadDto.setAutoOverwrite(true);

        LakeCloudFileEntity fileEntity = lakeCloudFileService.uploadFile(fileUploadDto) ;

        // 保存到raw_metadata表中
        saveToRawMetadataTable(catalogEntity, fileEntity);
    }

    /**
     * 调试表的分区结构
     */
    private void debugTablePartition(Table table) {
        PartitionSpec spec = table.spec();
        log.info("=== 表分区结构调试 ===");
        log.info("表名: {}", table.name());
        log.info("是否分区: {}", spec.isPartitioned());
        log.info("分区字段数量: {}", spec.fields().size());

        for (PartitionField field : spec.fields()) {
            String sourceField = table.schema().findColumnName(field.sourceId());
            log.info("分区字段: {} -> {} (转换: {})",
                    sourceField,
                    field.name(),
                    field.transform());
        }
        log.info("====================");
    }

    /**
     * 保存文件元数据到raw_metadata表
     */
    private void saveToRawMetadataTable(CatalogEntity catalogEntity, LakeCloudFileEntity fileEntity) {
        try {
            // 获取表名和schema名称

            Long catalogId = catalogEntity.getId() ;
            CatalogTableEntity catalogTable = catalogTableService.getRawMetadataTable(catalogId) ;

            String schemaName = icebergTableUtils.getSchemeName(catalogTable.getSchemeName()) ;
            String tableName = "raw_metadata";

            // 加载Iceberg表
            Table table = icebergTableUtils.loadTable(schemaName, tableName);
            if (table == null) {
                log.error("无法加载Iceberg表: {}.{}", schemaName, tableName);
                throw new RuntimeException("表不存在: " + tableName);
            }

            debugTablePartition(table);

            // 创建记录
            GenericRecord record = createMetadataRecord(table, fileEntity, catalogEntity);

            // 保存到Iceberg表中
            saveRecordToIcebergTable(table, record);

            log.info("文件元数据保存成功，文件名: {}, 存储路径: {}",
                    fileEntity.getFileName(), fileEntity.getFileStorageId());

        } catch (Exception e) {
            log.error("保存文件元数据到raw_metadata表失败", e);
            throw new RuntimeException("保存文件元数据失败", e);
        }
    }

    private String generateGenericIcebergFilename() {
        // 通用格式：<sequence>-<uuid>.parquet
        return String.format("%05d-%s.parquet",
                System.currentTimeMillis() % 100000, // 时间戳为基础的序列号
                UUID.randomUUID());
    }

    /**
     * 将记录保存到Iceberg表中（简化版本）
     */
    private void saveRecordToIcebergTable(Table table, Record record) {
        try {

            // 使用DataWriter进行写入
            String filename = generateGenericIcebergFilename() ;
            OutputFile outputFile = table.io().newOutputFile(table.location() + "/data/" + filename);

            try (DataWriter<Record> writer = Parquet.writeData(outputFile)
                    .createWriterFunc(GenericParquetWriter::buildWriter)
                    .forTable(table)
                    .withSpec(PartitionSpec.unpartitioned())
                    .overwrite()
                    .build()) {

                writer.write(record);
                writer.close();

                // 完成写入并获取数据文件
                DataFile dataFile = writer.toDataFile();

                // 将数据文件添加到表中
                table.newAppend()
                        .appendFile(dataFile)
                        .commit();

                log.info("成功将记录保存到Iceberg表，文件: {}", dataFile.path());
            }
        } catch (IOException e) {
            log.error("保存记录到Iceberg表失败", e);
            throw new RuntimeException("保存记录失败", e);
        }
    }

    /**
     * 创建元数据记录
     */
    private GenericRecord createMetadataRecord(Table table, LakeCloudFileEntity fileEntity, CatalogEntity catalogEntity) {
        Schema schema = table.schema();
        GenericRecord record = GenericRecord.create(schema);

        // 设置字段值
        record.setField("id", IdUtil.getSnowflakeNextId());
        record.setField("file_name", fileEntity.getFileName());
        record.setField("file_path", fileEntity.getFileStorageId());
        record.setField("file_size", fileEntity.getFileSize() != null ? fileEntity.getFileSize() : 0L);
        record.setField("file_type", getFileType(fileEntity.getFileName()));
        record.setField("bucket_name", "ns_" + catalogEntity.getId());
        record.setField("status", "UPLOADED");

        LocalDateTime now = LocalDateTime.now();
        record.setField("create_time", LocalDateTime.now()) ;
        record.setField("update_time", LocalDateTime.now()) ;

        return record;
    }

    /**
     * 根据文件名获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "unknown";
    }

    /**
     * 将JSON数据插入到Iceberg表中
     */
    public void insertJsonDataToIceberg(InsertDataDto jsonData) {
        if (jsonData == null || jsonData.getTableId() == null || jsonData.getDataMap() == null) {
            throw new IllegalArgumentException("插入数据参数不能为空");
        }

        try {
            // 1. 根据tableId获取表信息
            CatalogTableEntity tableEntity = catalogTableService.getById(jsonData.getTableId());
            if (tableEntity == null) {
                throw new RuntimeException("表不存在，表ID: " + jsonData.getTableId());
            }

            // 2. 获取schema名称和表名
            String schemaName = icebergTableUtils.getSchemeName(tableEntity);
            String tableName = tableEntity.getTableName();

            // 3. 加载Iceberg表
            Table table = icebergTableUtils.loadTable(schemaName, tableName);
            if (table == null) {
                throw new RuntimeException("Iceberg表不存在: " + schemaName + "." + tableName);
            }

            // 4. 将JSON数据转换为Iceberg记录并插入
            insertDataToTable(table, jsonData.getDataMap());

            log.info("成功将{}条数据插入到表: {}.{}",
                    jsonData.getDataMap().size(), schemaName, tableName);

        } catch (Exception e) {
            log.error("插入JSON数据到Iceberg表失败，表ID: {}", jsonData.getTableId(), e);
            throw new RuntimeException("插入数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将数据映射插入到Iceberg表中
     */
    private void insertDataToTable(Table table, List<Map<String, Object>> dataMap) {
        if (dataMap == null || dataMap.isEmpty()) {
            log.warn("数据映射为空，跳过插入");
            return;
        }

        try {
            Schema schema = table.schema();

            // 创建Append实例
            AppendFiles append = table.newAppend();

            for (Map<String, Object> data : dataMap) {
                // 创建记录
                Record record = createRecordFromMap(schema, data);

                // 创建数据文件并添加到append操作
                DataFile dataFile = createDataFile(table, record);
                append.appendFile(dataFile);
            }

            // 提交所有更改
            append.commit();

        } catch (Exception e) {
            log.error("插入数据到表失败", e);
            throw new RuntimeException("数据插入失败", e);
        }
    }

    /**
     * 从Map创建Iceberg记录（支持类型转换）
     */
    private Record createRecordFromMap(Schema schema, Map<String, Object> data) {
        GenericRecord record = GenericRecord.create(schema);

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            // 检查字段是否存在于schema中
            Types.NestedField field = schema.findField(fieldName);
            if (field != null) {
                try {
                    Object convertedValue = IcebergConvertUtils.convertValueToIcebergType(value, field.type());
                    record.setField(fieldName, convertedValue);
                } catch (Exception e) {
                    log.warn("字段 {} 值转换失败: {}, 使用null值", fieldName, e.getMessage());
                    record.setField(fieldName, null);
                }
            } else {
                log.warn("字段 {} 不在表schema中，跳过", fieldName);
            }
        }

        return record;
    }


    /**
     * 创建数据文件（简化版本，实际生产环境可能需要批量处理）
     */
    private DataFile createDataFile(Table table, Record record) throws IOException {
        String filename = generateGenericIcebergFilename();
        OutputFile outputFile = table.io().newOutputFile(table.location() + "/data/" + filename);

        try (DataWriter<Record> writer = Parquet.writeData(outputFile)
                .createWriterFunc(GenericParquetWriter::buildWriter)
                .forTable(table)
                .withSpec(PartitionSpec.unpartitioned())
                .overwrite()
                .build()) {

            writer.write(record);
            writer.close();

            return writer.toDataFile();
        }
    }

}
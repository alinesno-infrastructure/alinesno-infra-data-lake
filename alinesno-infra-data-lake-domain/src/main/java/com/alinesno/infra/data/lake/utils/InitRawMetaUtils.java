package com.alinesno.infra.data.lake.utils;

import com.alinesno.infra.data.lake.api.CreateCatalogTableDto;
import com.alinesno.infra.data.lake.api.TableFieldDto;
import com.alinesno.infra.data.lake.api.TablePropertyDto;
import com.alinesno.infra.data.lake.entity.CatalogTableEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 初始化原始数据元数据表
 */
public class InitRawMetaUtils {

    @NotNull
    public static CreateCatalogTableDto getCreateCatalogTableDto(long catalogId, CatalogTableEntity directory) {
        // 创建元数据表DTO
        CreateCatalogTableDto metaTableDto = new CreateCatalogTableDto();
        metaTableDto.setCatalogId(catalogId);
        if(directory != null){
            metaTableDto.setParentId(directory.getId());
        }
        metaTableDto.setTableName("raw_metadata");
        metaTableDto.setFormat("ICEBERG");
        metaTableDto.setDescription("原始数据元数据表，管理非结构化数据的元信息（如Oss存储的文件）");

        // 定义元数据表的字段
        List<TableFieldDto> fields = new ArrayList<>();

        // 主键字段
        TableFieldDto idField = new TableFieldDto();
        idField.setName("id");
        idField.setDataType("bigint");
        idField.setIsPrimary(true);
        idField.setNotNull(true);
        idField.setDescription("唯一标识");
        fields.add(idField);

        // 文件名称字段
        TableFieldDto fileNameField = new TableFieldDto();
        fileNameField.setName("file_name");
        fileNameField.setDataType("varchar");
        fileNameField.setLength("255");
        fileNameField.setDescription("文件名称");
        fields.add(fileNameField);

        // 文件路径字段
        TableFieldDto filePathField = new TableFieldDto();
        filePathField.setName("file_path");
        filePathField.setDataType("varchar");
        filePathField.setLength("500");
        filePathField.setDescription("文件存储路径");
        fields.add(filePathField);

        // 文件大小字段
        TableFieldDto fileSizeField = new TableFieldDto();
        fileSizeField.setName("file_size");
        fileSizeField.setDataType("bigint");
        fileSizeField.setDescription("文件大小（字节）");
        fields.add(fileSizeField);

        // 文件类型字段
        TableFieldDto fileTypeField = new TableFieldDto();
        fileTypeField.setName("file_type");
        fileTypeField.setDataType("varchar");
        fileTypeField.setLength("50");
        fileTypeField.setDescription("文件类型（如csv, parquet, json等）");
        fields.add(fileTypeField);

        // 存储bucket字段
        TableFieldDto bucketField = new TableFieldDto();
        bucketField.setName("bucket_name");
        bucketField.setDataType("varchar");
        bucketField.setLength("100");
        bucketField.setDescription("存储桶名称");
        fields.add(bucketField);

        // 状态字段
        TableFieldDto statusField = new TableFieldDto();
        statusField.setName("status");
        statusField.setDataType("varchar");
        statusField.setLength("20");
        statusField.setDescription("文件状态（如UPLOADED, PROCESSED, DELETED）");
        fields.add(statusField);

        // 创建时间字段（分区字段）
        TableFieldDto createTimeField = new TableFieldDto();
        createTimeField.setName("create_time");
        createTimeField.setDataType("timestamp");
        createTimeField.setIsPartition(true);
        createTimeField.setDescription("文件创建时间");
        fields.add(createTimeField);

        // 更新时间字段
        TableFieldDto updateTimeField = new TableFieldDto();
        updateTimeField.setName("update_time");
        updateTimeField.setDataType("timestamp");
        updateTimeField.setDescription("最后更新时间");
        fields.add(updateTimeField);

        metaTableDto.setFields(fields);

        // 设置表属性
        List<TablePropertyDto> props = new ArrayList<>();

        TablePropertyDto prop1 = new TablePropertyDto();
        prop1.setKey("table_type");
        prop1.setValue("metadata");
        props.add(prop1);

        TablePropertyDto prop2 = new TablePropertyDto();
        prop2.setKey("managed_by");
        prop2.setValue("data_lake_system");
        props.add(prop2);

//        metaTableDto.setProps(props);
        return metaTableDto;
    }

}

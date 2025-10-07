package com.alinesno.infra.data.lake.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 表字段DTO
 */
@Data
public class TableFieldDto {

    @NotBlank(message = "字段名称不能为空")
    private String name;

    private Boolean isPrimary = false;

    private Boolean notNull = false;

    private Boolean isPartition = false;

    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    private String length;

    private String description;
}
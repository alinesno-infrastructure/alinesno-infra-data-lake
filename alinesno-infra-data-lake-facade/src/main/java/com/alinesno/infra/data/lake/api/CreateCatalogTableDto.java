package com.alinesno.infra.data.lake.api;

import com.alinesno.infra.common.facade.base.BaseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 创建目录表DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateCatalogTableDto extends BaseDto {

    @NotNull(message = "目录ID不能为空")
    private Long catalogId;

    @NotNull(message = "父级ID不能为空")
    private Long parentId ;

    @NotBlank(message = "表名称不能为空")
    private String tableName;

    @NotBlank(message = "表格式不能为空")
    private String format;

    @NotBlank(message = "表描述不能为空")
    private String description;

    @Valid
    @NotEmpty(message = "至少需要一个表字段")
    @Size(min = 1, message = "至少需要一个表字段")
    private List<TableFieldDto> fields;

    @Valid
    private List<TablePropertyDto> props;

}
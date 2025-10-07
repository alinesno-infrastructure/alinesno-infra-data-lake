package com.alinesno.infra.data.lake.api;

import com.alinesno.infra.common.facade.base.BaseDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据目录DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CatalogDto extends BaseDto {

    @NotBlank(message = "目录名称不能为空")
    @Size(max = 100, message = "目录名称长度不能超过100个字符")
    private String catalogName;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @Size(max = 50, message = "图标标识长度不能超过50个字符")
    private String icon;
}
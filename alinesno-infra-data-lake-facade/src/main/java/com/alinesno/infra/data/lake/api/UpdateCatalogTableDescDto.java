package com.alinesno.infra.data.lake.api;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新目录表描述的 DTO 类
 */
@Data
public class UpdateCatalogTableDescDto {

    @NotNull(message = "id不能为空")
    private Long id;

    @NotNull(message = "描述不能为空")
    private String description;

}
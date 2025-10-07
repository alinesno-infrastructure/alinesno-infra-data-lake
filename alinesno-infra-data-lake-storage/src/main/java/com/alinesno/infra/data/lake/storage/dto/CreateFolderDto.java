package com.alinesno.infra.data.lake.storage.dto;

import com.alinesno.infra.common.facade.base.BaseDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 创建文件夹DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateFolderDto extends BaseDto {

    /**
     * 文件夹名称
     */
    @NotBlank(message = "文件夹名称不能为空")
    private String folderName;

    /**
     * 父目录ID（0表示根目录）
     */
    @NotNull(message = "父目录ID不能为空")
    private Long parentId;
}
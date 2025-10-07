package com.alinesno.infra.data.lake.storage.dto;

import com.alinesno.infra.common.facade.base.BaseDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FileUploadDto extends BaseDto {
    
    /**
     * 上传的文件
     */
    @NotNull(message = "文件不能为空")
    private MultipartFile file;
    
    /**
     * 父目录ID
     */
    @NotNull(message = "父目录ID不能为空")
    private Long parentId;

    /**
     * 是否自动覆盖同名文件
     */
    private boolean autoOverwrite ;
    
}
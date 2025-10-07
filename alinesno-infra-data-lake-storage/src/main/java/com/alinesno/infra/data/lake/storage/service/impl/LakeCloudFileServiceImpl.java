package com.alinesno.infra.data.lake.storage.service.impl;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.core.utils.DateUtils;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.data.lake.adapter.service.CloudStorageConsumer;
import com.alinesno.infra.data.lake.service.IStorageFileService;
import com.alinesno.infra.data.lake.storage.dto.CreateFolderDto;
import com.alinesno.infra.data.lake.storage.dto.FileStatisticsDto;
import com.alinesno.infra.data.lake.storage.dto.FileUploadDto;
import com.alinesno.infra.data.lake.storage.entity.LakeCloudFileEntity;
import com.alinesno.infra.data.lake.storage.enums.FileTypeEnum;
import com.alinesno.infra.data.lake.storage.mapper.LakeCloudFileMapper;
import com.alinesno.infra.data.lake.storage.service.ILakeCloudFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 存储文件服务实现类
 */
@Slf4j
@Service
public class LakeCloudFileServiceImpl extends IBaseServiceImpl<LakeCloudFileEntity, LakeCloudFileMapper> implements ILakeCloudFileService {

    @Autowired
    private IStorageFileService storageFileService ;

    @Autowired
    private CloudStorageConsumer cloudStorageConsumer;

    @Override
    public boolean folderExists(CreateFolderDto dto) {
        // 检查文件夹名称是否已存在（在同一父目录下）
        return this.findAll().stream()
                .anyMatch(file ->
                        file.getParentId().equals(dto.getParentId()) &&
                                file.getOrgId().equals(dto.getOrgId()) &&
                                file.getFileName().equals(dto.getFolderName()) &&
                                file.getIsDirectory() == 1
                );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LakeCloudFileEntity createFolder(CreateFolderDto dto) {
        // 检查文件夹名称是否已存在（在同一父目录下）
        boolean exists = folderExists(dto) ;
        if (exists) {
            throw new RuntimeException("该目录下已存在同名文件夹");
        }

        // 创建文件夹实体
        LakeCloudFileEntity folder = getLakeCloudFileEntity(dto);

        // 保存到数据库
        save(folder);

        return folder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LakeCloudFileEntity uploadFile(FileUploadDto dto) {
        MultipartFile multipartFile = dto.getFile();

        // 安全校验
        validateFileSecurity(multipartFile);

        // 检查文件名是否已存在
        boolean exists = this.findAll().stream()
                .anyMatch(file ->
                        file.getParentId().equals(dto.getParentId()) &&
                                file.getFileName().equals(multipartFile.getOriginalFilename()) &&
                                file.getIsDirectory() == 0
                );

        if (exists) {
            if(!dto.isAutoOverwrite()){
                throw new RuntimeException("该目录下已存在同名文件");
            }else{
                // 删除同名文件
                LambdaQueryWrapper<LakeCloudFileEntity> queryWrapper = new LambdaQueryWrapper<LakeCloudFileEntity>()
                        .eq(LakeCloudFileEntity::getParentId, dto.getParentId())
                        .eq(LakeCloudFileEntity::getFileName, multipartFile.getOriginalFilename())
                        .eq(LakeCloudFileEntity::getIsDirectory, 0);
                remove(queryWrapper) ;
            }
        }

        try {
            // 创建文件实体
            LakeCloudFileEntity fileEntity = new LakeCloudFileEntity();

            // 设置文件属性
            fileEntity.setOperatorId(dto.getOperatorId());
            fileEntity.setOrgId(dto.getOrgId());
            fileEntity.setDepartmentId(dto.getDepartmentId());

            fileEntity.setFileName(multipartFile.getOriginalFilename());
            fileEntity.setParentId(dto.getParentId());
            fileEntity.setIsDirectory(FileTypeEnum.FILE.getCode());
            fileEntity.setFileSize(multipartFile.getSize());

            // 获取文件扩展名和类型
            String originalFilename = multipartFile.getOriginalFilename();
            String fileExtension = FileUtil.getSuffix(originalFilename);
            fileEntity.setFileExtension(fileExtension);
            fileEntity.setFileType(getFileType(fileExtension));

            // 临时文件存储，创建临时文件处理完成之后，删除临时文件
            File tempFile = FileUtil.createTempFile(multipartFile.getOriginalFilename(), true);
            multipartFile.transferTo(tempFile);
            String uploadResult = cloudStorageConsumer.upload(tempFile , DateUtils.getDate()).getData() ;
            fileEntity.setFileStorageId(uploadResult) ;

            // 计算文件MD5（这里需要实际实现MD5计算）
            fileEntity.setFileMd5(calculateFileMd5(multipartFile));

            fileEntity.setShareStatus(0);
            fileEntity.setSharePassword("");
            fileEntity.setShareExpireTime(null);

            // 保存到数据库
            save(fileEntity);

            // 这里应该添加实际的文件存储逻辑，比如保存到本地磁盘或云存储
            // saveFileToStorage(multipartFile, fileEntity.getFilePath());

            return fileEntity;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public List<LakeCloudFileEntity> batchUploadFile(MultipartFile[] files, Long parentId) {
        List<LakeCloudFileEntity> uploadedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                FileUploadDto dto = new FileUploadDto();
                dto.setFile(file);
                dto.setParentId(parentId);

                try {
                    LakeCloudFileEntity fileEntity = uploadFile(dto);
                    uploadedFiles.add(fileEntity);
                } catch (Exception e) {
                    log.error("文件 {} 上传失败", file.getOriginalFilename(), e);
                }
            }
        }

        return uploadedFiles;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        LakeCloudFileEntity file = getById(id);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }

        if (FileTypeEnum.isDirectory(file.getIsDirectory())) {
            throw new RuntimeException("不能使用此接口删除文件夹，请使用删除文件夹接口");
        }

        // 这里应该添加实际的文件删除逻辑
        // deleteFileFromStorage(file.getFilePath());

        // 删除数据库记录
        removeById(id);
    }

    @Override
    public List<LakeCloudFileEntity> getFolderTree() {
        // 查询所有文件夹（isDirectory = 1）
        LambdaQueryWrapper<LakeCloudFileEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LakeCloudFileEntity::getIsDirectory , FileTypeEnum.DIRECTORY.getCode());
        queryWrapper.orderByAsc(LakeCloudFileEntity::getParentId);

        return list(queryWrapper);
    }

    @Override
    public void validateFileSecurity(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new RuntimeException("文件名不能为空");
        }

        // 检查文件扩展名
        String fileExtension = FileUtil.getSuffix(originalFilename).toLowerCase();
//        if (FORBIDDEN_FILE_TYPES.contains(fileExtension)) {
//            throw new RuntimeException("不允许上传该类型的文件");
//        }
//
//        if (!ALLOWED_FILE_TYPES.contains(fileExtension)) {
//            throw new RuntimeException("不支持的文件类型");
//        }

        try {
            // 使用Hutool检查实际文件类型（防止文件扩展名伪造）
            String actualFileType = FileTypeUtil.getType(file.getInputStream());
            if (actualFileType == null) {
                // 如果不能识别文件类型，进行安全警告
                log.warn("无法识别的文件类型: {}", originalFilename);
            }

            // 这里可以添加更多的安全校验，比如检查文件内容是否安全

        } catch (IOException e) {
            throw new RuntimeException("文件校验失败", e);
        }
    }

    private String getFileType(String fileExtension) {
        // 根据文件扩展名返回文件类型
        if (Arrays.asList("jpg", "jpeg", "png", "gif", "bmp").contains(fileExtension)) {
            return "image";
        } else if (Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx").contains(fileExtension)) {
            return "document";
        } else if (Arrays.asList("txt", "csv").contains(fileExtension)) {
            return "text";
        } else if (Arrays.asList("zip", "rar", "7z").contains(fileExtension)) {
            return "archive";
        } else {
            return "other";
        }
    }

    private String calculateFileMd5(MultipartFile file) throws IOException {
        // 使用Hutool计算文件MD5
        // 这里需要实际实现MD5计算逻辑
        // return SecureUtil.md5(file.getInputStream());
        return "mock_md5_value"; // 临时返回mock值
    }

    @NotNull
    private static LakeCloudFileEntity getLakeCloudFileEntity(CreateFolderDto dto) {
        LakeCloudFileEntity folder = new LakeCloudFileEntity();

        folder.setOrgId(dto.getOrgId());
        folder.setOperatorId(dto.getOperatorId());
        folder.setDepartmentId(dto.getDepartmentId());

        folder.setFileName(dto.getFolderName());
        folder.setParentId(dto.getParentId());
        folder.setIsDirectory(FileTypeEnum.DIRECTORY.getCode());
        folder.setFileSize(0L);
        folder.setFileType("directory");
        folder.setFileExtension(StringUtils.EMPTY);
        folder.setFileStorageId(StringUtils.EMPTY);
        folder.setFileMd5(StringUtils.EMPTY);
        folder.setShareStatus(0);
        folder.setSharePassword("");
        folder.setShareExpireTime(null);
        return folder;
    }

    @Override
    public FileStatisticsDto getFileStatistics(Long folderId, PermissionQuery permissionQuery) {
        FileStatisticsDto statistics = new FileStatisticsDto();
        statistics.setFolderId(folderId);

        // 获取文件夹名称
        String folderName = "根目录";
        if (folderId != null && folderId > 0) {
            LakeCloudFileEntity folder = getById(folderId);
            if (folder != null && FileTypeEnum.isDirectory(folder.getIsDirectory())) {
                folderName = folder.getFileName();
            }
        }
        statistics.setFolderName(folderName);

        // 构建查询条件
        LambdaQueryWrapper<LakeCloudFileEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LakeCloudFileEntity::getOrgId, permissionQuery.getOrgId());

        // 如果是根目录，查询所有文件；否则查询指定文件夹及其子文件夹下的文件
        if (folderId != null && folderId == 0) {
            // 根目录，查询所有非文件夹文件
            queryWrapper.eq(LakeCloudFileEntity::getIsDirectory, FileTypeEnum.FILE.getCode());
        } else {
            // 查询指定文件夹及其所有子文件夹下的文件
            List<Long> folderIds = getSubFolderIds(folderId);
            folderIds.add(folderId); // 包含当前文件夹
            queryWrapper.in(LakeCloudFileEntity::getParentId, folderIds)
                    .eq(LakeCloudFileEntity::getIsDirectory, FileTypeEnum.FILE.getCode());
        }

        // 统计总文件数量和大小
        List<LakeCloudFileEntity> files = list(queryWrapper);
        statistics.setTotalFiles((long) files.size());

        long totalSizeBytes = files.stream()
                .mapToLong(LakeCloudFileEntity::getFileSize)
                .sum();
        statistics.setTotalSize(formatFileSize(totalSizeBytes));

        // 统计今日上传文件数量
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LambdaQueryWrapper<LakeCloudFileEntity> todayQuery = queryWrapper.clone();
        todayQuery.ge(LakeCloudFileEntity::getAddTime, todayStart);
        statistics.setTodayUploads(count(todayQuery));

        // 统计本周上传文件数量
        LocalDateTime weekStart = todayStart.with(DayOfWeek.MONDAY);
        LambdaQueryWrapper<LakeCloudFileEntity> weekQuery = queryWrapper.clone();
        weekQuery.ge(LakeCloudFileEntity::getAddTime, weekStart);
        statistics.setWeekUploads(count(weekQuery));

        return statistics;
    }

    @Override
    public LakeCloudFileEntity getFolderByName(CreateFolderDto createFolderDto) {
        LambdaQueryWrapper<LakeCloudFileEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LakeCloudFileEntity::getParentId, createFolderDto.getParentId())
                .eq(LakeCloudFileEntity::getOrgId, createFolderDto.getOrgId())
                .eq(LakeCloudFileEntity::getFileName, createFolderDto.getFolderName())
                .eq(LakeCloudFileEntity::getIsDirectory, FileTypeEnum.DIRECTORY.getCode());
        return getOne(queryWrapper) ;
    }

    /**
     * 获取指定文件夹的所有子文件夹ID
     */
    private List<Long> getSubFolderIds(Long parentId) {
        List<Long> subFolderIds = new ArrayList<>();

        LambdaQueryWrapper<LakeCloudFileEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LakeCloudFileEntity::getParentId, parentId)
                .eq(LakeCloudFileEntity::getIsDirectory, FileTypeEnum.DIRECTORY.getCode());

        List<LakeCloudFileEntity> subFolders = list(queryWrapper);

        for (LakeCloudFileEntity folder : subFolders) {
            subFolderIds.add(folder.getId());
            // 递归获取子文件夹的子文件夹
            subFolderIds.addAll(getSubFolderIds(folder.getId()));
        }

        return subFolderIds;
    }

    /**
     * 格式化文件大小（自动选择合适单位）
     */
    private String formatFileSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            return "0 B";
        }

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeBytes) / Math.log10(1024));

        if (digitGroups >= units.length) {
            digitGroups = units.length - 1;
        }

        return new DecimalFormat("#,##0.#")
                .format(sizeBytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
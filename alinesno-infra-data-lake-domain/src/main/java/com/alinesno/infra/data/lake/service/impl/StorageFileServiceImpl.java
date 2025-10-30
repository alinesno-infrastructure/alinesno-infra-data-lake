package com.alinesno.infra.data.lake.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.extra.spring.SpringUtil;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.lake.entity.StorageFileEntity;
import com.alinesno.infra.data.lake.mapper.StorageFileMapper;
import com.alinesno.infra.data.lake.properties.IcebergProperties;
import com.alinesno.infra.data.lake.service.IStorageFileService;
import com.alinesno.infra.data.lake.storage.OSSBucketService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 存储文件服务实现类
 */
@Slf4j
@Service
public class StorageFileServiceImpl extends IBaseServiceImpl<StorageFileEntity, StorageFileMapper> implements IStorageFileService {

    @Value("${iceberg.storage-type}")
    private String storageType ;  // 存储类型

    @Autowired
    private IcebergProperties icebergProperties ;

    @Override
    public String createBucket(String bucketName) {

        OSSBucketService ossBucketService = getOssBucketService();

        boolean b = ossBucketService.createBucket(bucketName);
        Assert.isTrue(b, "创建bucket失败");

        log.debug("创建bucket：{}" , bucketName) ;
        return ossBucketService.getBucketName(bucketName);
    }

    private OSSBucketService getOssBucketService() {
        OSSBucketService ossBucketService = SpringUtil.getBean(storageType + "-oss-service");
        Assert.notNull(ossBucketService, "请配置存储服务或者不支持存储类型") ;
        return ossBucketService;
    }

    @Override
    public void deleteBucket(String bucketName) {
       OSSBucketService ossBucketService = getOssBucketService();
       boolean b = ossBucketService.deleteBucket(bucketName);

       // Assert.isTrue(b, "删除bucket失败");
        if(!b){
            log.warn("删除bucket失败：{}" , bucketName);
        }

       log.debug("删除bucket：{}" , bucketName) ;
    }

    @Override
    public boolean createDirectory(String bucketSuffix, String directoryPath) {
        OSSBucketService ossBucketService = getOssBucketService();
        return ossBucketService.createDirectory(bucketSuffix, directoryPath);
    }

    @Override
    public boolean deleteDirectory(String bucketSuffix, String directoryPath) {
        return false;
    }


    @Override
    public boolean renameDirectory(String bucketSuffix, String oldDirectoryPath, String newDirectoryPath) {
        OSSBucketService ossBucketService = getOssBucketService();
        return ossBucketService.renameDirectory(bucketSuffix, oldDirectoryPath, newDirectoryPath);
    }

    @Override
    public boolean uploadFile(String bucketSuffix, String filePath, MultipartFile file) {
        try {
            OSSBucketService ossBucketService = getOssBucketService();
            return ossBucketService.uploadFile(bucketSuffix, filePath, file.getInputStream(), file.getContentType());
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return false;
        }
    }

    @Override
    public void downloadFile(String bucketSuffix, String filePath, HttpServletResponse response) {
        OSSBucketService ossBucketService = getOssBucketService();
        try (InputStream inputStream = ossBucketService.downloadFile(bucketSuffix, filePath)) {
            if (inputStream == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + java.net.URLEncoder.encode(getFileName(filePath), StandardCharsets.UTF_8) + "\"");

            // 将文件流写入响应
            byte[] buffer = new byte[8192];
            int bytesRead;
            OutputStream outputStream = response.getOutputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (IOException e) {
            log.error("文件下载失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean deleteFile(String bucketSuffix, String filePath) {
        OSSBucketService ossBucketService = getOssBucketService();
        return ossBucketService.deleteFile(bucketSuffix, filePath);
    }

    @Override
    public String upload(File tempFile, String date) {
        OSSBucketService ossBucketService = getOssBucketService();
        String bucketName = icebergProperties.getWarehousePath() + "-file" ;
        return ossBucketService.uploadFileByDate(bucketName , tempFile, date) ;
    }

    private String getFileName(String filePath) {
        if (filePath.contains("/")) {
            return filePath.substring(filePath.lastIndexOf("/") + 1);
        }
        return filePath;
    }



}

package com.alinesno.infra.data.lake.storage;

import com.alinesno.infra.data.lake.config.MinIOConfig;
import io.minio.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MinIOBucketService
 */
@Slf4j
@Service("minio-oss-service")
public class MinIOBucketService implements OSSBucketService  {

    private final MinioClient minioClient;
    private final String storageBucketPrefix;

    @Autowired
    public MinIOBucketService(MinioClient minioClient, MinIOConfig minIOConfig) {
        this.minioClient = minioClient;
        this.storageBucketPrefix = minIOConfig.getStorageBucket();
    }

    /**
     * 创建以storageBucket为前缀的Bucket
     * @param suffix Bucket名称的后缀
     * @return 创建成功返回true，否则返回false
     */
    @Override
    public boolean createBucket(String suffix) {
        try {
            String bucketName = getBucketName(suffix);

            // 检查Bucket是否已存在
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (exists) {
                return false; // Bucket已存在
            }

            // 创建Bucket
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            return true;
        } catch (Exception e) {
            // 处理异常
            log.error("创建Bucket失败：" , e) ;
            return false;
        }
    }

    /**
     * 删除以storageBucket为前缀的Bucket
     * @param suffix Bucket名称的后缀
     * @return 删除成功返回true，否则返回false
     */
    @Override
    public boolean deleteBucket(String suffix) {

        Assert.hasLength(suffix, "Bucket名称不能为空");

        try {
            String bucketName = getBucketName(suffix);

            // 检查Bucket是否存在
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                return true ; // Bucket不存在
            }

            // 删除Bucket
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            return true;
        } catch (Exception e) {
            // 处理异常
            log.error("删除Bucket失败：" , e) ;
            return false;
        }
    }

    /**
     * 检查Bucket是否存在
     * @param suffix Bucket名称的后缀
     * @return 存在返回true，否则返回false
     */
    @Override
    public boolean bucketExists(String suffix) {
        try {
            String bucketName = getBucketName(suffix);
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.error("检查Bucket是否存在失败：" , e) ;
            return false;
        }
    }

    /**
     * 生成完整的Bucket名称（前缀+后缀）
     * @param suffix 后缀
     * @return 完整的Bucket名称
     */
    @Override
    public String getBucketName(String suffix) {
        if (suffix == null || suffix.trim().isEmpty()) {
            return storageBucketPrefix;
        }
        return storageBucketPrefix + "-" + suffix;
    }

    /**
     * 在指定Bucket中创建目录（支持多层级）
     * @param bucketSuffix Bucket名称的后缀
     * @param directoryPath 目录路径，支持多层级（如：folder1/folder2/subfolder/）
     * @return 创建成功返回true，否则返回false
     */
    @Override
    public boolean createDirectory(String bucketSuffix, String directoryPath) {
        Assert.hasLength(bucketSuffix, "Bucket后缀不能为空");
        Assert.hasLength(directoryPath, "目录路径不能为空");

        try {
            String bucketName = getBucketName(bucketSuffix);

            // 确保目录路径以斜杠结尾
            String normalizedPath = normalizeDirectoryPath(directoryPath);

            // 在MinIO中，目录是通过创建一个以斜杠结尾的空对象来实现的
            // 使用空的字节数组输入流而不是null
            byte[] emptyContent = new byte[0];
            ByteArrayInputStream emptyStream = new ByteArrayInputStream(emptyContent);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(normalizedPath)
                            .stream(emptyStream, emptyContent.length, -1)
                            .build()
            );

            log.info("目录创建成功: {}/{}", bucketName, normalizedPath);
            return true;
        } catch (Exception e) {
            log.error("创建目录失败: bucketSuffix={}, directoryPath={}", bucketSuffix, directoryPath, e);
            return false;
        }
    }

    /**
     * 删除指定Bucket中的目录（支持多层级）
     * @param bucketSuffix Bucket名称的后缀
     * @param directoryPath 目录路径，支持多层级（如：folder1/folder2/subfolder/）
     * @return 删除成功返回true，否则返回false
     */
    @Override
    public boolean deleteDirectory(String bucketSuffix, String directoryPath) {
        Assert.hasLength(bucketSuffix, "Bucket后缀不能为空");
        Assert.hasLength(directoryPath, "目录路径不能为空");

        try {
            String bucketName = getBucketName(bucketSuffix);
            String normalizedPath = normalizeDirectoryPath(directoryPath);

            // 检查目录是否存在
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                log.warn("Bucket不存在: {}", bucketName);
                return false;
            }

            // 列出目录下的所有对象（包括子目录）
            List<DeleteObject> objectsToDelete = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(normalizedPath)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                objectsToDelete.add(new DeleteObject(item.objectName()));
            }

            if (objectsToDelete.isEmpty()) {
                log.info("目录为空或不存在: {}/{}", bucketName, normalizedPath);
                return true;
            }

            // 批量删除目录下的所有对象
            minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objectsToDelete)
                            .build()
            );

            log.info("目录删除成功: {}/{}，共删除{}个对象", bucketName, normalizedPath, objectsToDelete.size());
            return true;
        } catch (Exception e) {
            log.error("删除目录失败: bucketSuffix={}, directoryPath={}", bucketSuffix, directoryPath, e);
            return false;
        }
    }

    /**
     * 规范化目录路径，确保以斜杠结尾
     * @param directoryPath 原始目录路径
     * @return 规范化后的目录路径
     */
    private String normalizeDirectoryPath(String directoryPath) {
        String normalized = directoryPath.trim();

        // 移除开头的斜杠
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // 确保以斜杠结尾
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }

        return normalized;
    }

    @Override
    public boolean renameDirectory(String bucketSuffix, String oldDirectoryPath, String newDirectoryPath) {
        Assert.hasLength(bucketSuffix, "Bucket后缀不能为空");
        Assert.hasLength(oldDirectoryPath, "旧目录路径不能为空");
        Assert.hasLength(newDirectoryPath, "新目录路径不能为空");

        try {
            String bucketName = getBucketName(bucketSuffix);
            String normalizedOldPath = normalizeDirectoryPath(oldDirectoryPath);
            String normalizedNewPath = normalizeDirectoryPath(newDirectoryPath);

            // 检查Bucket是否存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                log.warn("Bucket不存在: {}", bucketName);
                return false;
            }

            // 列出旧目录下的所有对象
            List<String> objectsToCopy = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(normalizedOldPath)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                objectsToCopy.add(item.objectName());
            }

            if (objectsToCopy.isEmpty()) {
                log.info("目录为空或不存在: {}/{}", bucketName, normalizedOldPath);
                return false;
            }

            // 复制所有对象到新路径并删除旧对象
            for (String oldKey : objectsToCopy) {
                String newKey = oldKey.replace(normalizedOldPath, normalizedNewPath);

                // 复制对象
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(bucketName)
                                .object(newKey)
                                .source(CopySource.builder()
                                        .bucket(bucketName)
                                        .object(oldKey)
                                        .build())
                                .build()
                );

                // 删除旧对象
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(oldKey)
                                .build()
                );
            }

            log.info("目录重命名成功: {}/{} -> {}/{}", bucketName, normalizedOldPath, bucketName, normalizedNewPath);
            return true;
        } catch (Exception e) {
            log.error("重命名目录失败: bucketSuffix={}, oldPath={}, newPath={}",
                    bucketSuffix, oldDirectoryPath, newDirectoryPath, e);
            return false;
        }
    }

    @Override
    public boolean uploadFile(String bucketSuffix, String filePath, java.io.InputStream inputStream, String contentType) {
        Assert.hasLength(bucketSuffix, "Bucket后缀不能为空");
        Assert.hasLength(filePath, "文件路径不能为空");
        Assert.notNull(inputStream, "文件输入流不能为空");

        try {
            String bucketName = getBucketName(bucketSuffix);

            // 检查Bucket是否存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                log.warn("Bucket不存在: {}", bucketName);
                return false;
            }

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .stream(inputStream, -1, 10485760) // 10MB part size
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );

            log.info("文件上传成功: {}/{}", bucketName, filePath);
            return true;
        } catch (Exception e) {
            log.error("文件上传失败: bucketSuffix={}, filePath={}", bucketSuffix, filePath, e);
            return false;
        }
    }

    @Override
    public java.io.InputStream downloadFile(String bucketSuffix, String filePath) {
        Assert.hasLength(bucketSuffix, "Bucket后缀不能为空");
        Assert.hasLength(filePath, "文件路径不能为空");

        try {
            String bucketName = getBucketName(bucketSuffix);

            // 检查Bucket是否存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                log.warn("Bucket不存在: {}", bucketName);
                return null;
            }

            // 检查文件是否存在
            try {
                minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filePath)
                        .build());
            } catch (Exception e) {
                log.warn("文件不存在: {}/{}", bucketName, filePath);
                return null;
            }

            // 下载文件
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build()
            );

        } catch (Exception e) {
            log.error("文件下载失败: bucketSuffix={}, filePath={}", bucketSuffix, filePath, e);
            return null;
        }
    }

    @Override
    public boolean deleteFile(String bucketSuffix, String filePath) {
        Assert.hasLength(bucketSuffix, "Bucket后缀不能为空");
        Assert.hasLength(filePath, "文件路径不能为空");

        try {
            String bucketName = getBucketName(bucketSuffix);

            // 检查Bucket是否存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                log.warn("Bucket不存在: {}", bucketName);
                return false;
            }

            // 删除文件
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build()
            );

            log.info("文件删除成功: {}/{}", bucketName, filePath);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: bucketSuffix={}, filePath={}", bucketSuffix, filePath, e);
            return false;
        }
    }

    @Override
    public String uploadFileByDate(String bucketSuffix, File tempFile, String date) {
        Assert.hasLength(bucketSuffix, "Bucket后缀不能为空");
        Assert.notNull(tempFile, "文件不能为空");
        Assert.hasLength(date, "日期不能为空");

        try {
            String bucketName = getBucketName(bucketSuffix);

            // 检查Bucket是否存在，如果不存在则创建
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建Bucket: {}", bucketName);
            }

            // 根据日期生成目录结构（格式：yyyy/MM/dd/）
            String datePath = formatDatePath(date);

            // 生成最终的文件路径
            String fileName = tempFile.getName();
            String filePath = datePath + fileName;

            // 上传文件
            try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(filePath)
                                .stream(fileInputStream, tempFile.length(), -1)
                                .contentType(getContentType(fileName))
                                .build()
                );
            }

            // 构建完整的存储ID（包含bucket名称和文件路径）
            String storageId = bucketName + "/" + filePath;

            log.info("文件按日期上传成功: {}, 文件大小: {} bytes", storageId, tempFile.length());
            return storageId;

        } catch (Exception e) {
            log.error("按日期上传文件失败: bucketSuffix={}, file={}, date={}",
                    bucketSuffix, tempFile.getName(), date, e);
            return null;
        }
    }

    /**
     * 根据日期生成目录路径
     * @param date 日期字符串（格式应为 yyyy-MM-dd 或类似格式）
     * @return 格式化后的目录路径（如：2023/10/25/）
     */
    private String formatDatePath(String date) {
        try {
            // 尝试解析常见日期格式
            String[] dateParts;
            if (date.contains("-")) {
                dateParts = date.split("-");
            } else if (date.contains("/")) {
                dateParts = date.split("/");
            } else if (date.length() == 8) { // yyyyMMdd格式
                dateParts = new String[]{
                        date.substring(0, 4),
                        date.substring(4, 6),
                        date.substring(6, 8)
                };
            } else {
                throw new IllegalArgumentException("不支持的日期格式: " + date);
            }

            if (dateParts.length >= 3) {
                return dateParts[0] + "/" + dateParts[1] + "/" + dateParts[2] + "/";
            } else {
                throw new IllegalArgumentException("日期格式不完整: " + date);
            }
        } catch (Exception e) {
            log.warn("日期解析失败，使用原始日期作为目录: {}", date);
            return date + "/";
        }
    }

    /**
     * 根据文件名获取Content-Type
     * @param fileName 文件名
     * @return Content-Type字符串
     */
    private String getContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String lowerFileName = fileName.toLowerCase();

        if (lowerFileName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerFileName.endsWith(".csv")) {
            return "text/csv";
        } else if (lowerFileName.endsWith(".json")) {
            return "application/json";
        } else if (lowerFileName.endsWith(".xml")) {
            return "application/xml";
        } else if (lowerFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm")) {
            return "text/html";
        } else if (lowerFileName.endsWith(".js")) {
            return "application/javascript";
        } else if (lowerFileName.endsWith(".css")) {
            return "text/css";
        } else {
            return "application/octet-stream";
        }
    }

}

package com.alinesno.infra.data.lake.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云OSS存储桶服务
 */
@Slf4j
@Service("aliyun-oss-oss-service")
public class AliyunOSSBucketService implements OSSBucketService {

    private final OSS ossClient;
    private final String storageBucketPrefix;

    @Autowired
    public AliyunOSSBucketService(OSS ossClient, @Value("${iceberg.storage-bucket}") String storageBucketPrefix) {
        this.ossClient = ossClient;
        this.storageBucketPrefix = storageBucketPrefix;
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
            if (ossClient.doesBucketExist(bucketName)) {
                return false; // Bucket已存在
            }

            // 创建Bucket
            ossClient.createBucket(bucketName);
            return true;
        } catch (OSSException e) {
            System.err.println("OSS错误: " + e.getErrorMessage());
            return false;
        } catch (Exception e) {
            log.error("创建Bucket失败", e);
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
        try {
            String bucketName = getBucketName(suffix);

            // 检查Bucket是否存在
            if (!ossClient.doesBucketExist(bucketName)) {
                return false; // Bucket不存在
            }

            // 删除Bucket前需要先清空Bucket中的所有文件和目录
            ossClient.deleteObjects(new com.aliyun.oss.model.DeleteObjectsRequest(bucketName)
                    .withKeys(ossClient.listObjects(bucketName).getObjectSummaries()
                            .stream()
                            .map(obj -> obj.getKey())
                            .collect(java.util.stream.Collectors.toList())));

            // 删除Bucket
            ossClient.deleteBucket(bucketName);
            return true;
        } catch (OSSException e) {
            System.err.println("OSS错误: " + e.getErrorMessage());
            return false;
        } catch (Exception e) {
            log.error("删除Bucket失败", e);
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
            return ossClient.doesBucketExist(bucketName);
        } catch (OSSException e) {
            System.err.println("OSS错误: " + e.getErrorMessage());
            return false;
        } catch (Exception e) {
            log.error("检查Bucket是否存在失败", e);
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

            // 检查Bucket是否存在
            if (!ossClient.doesBucketExist(bucketName)) {
                log.warn("Bucket不存在: {}", bucketName);
                return false;
            }

            // 规范化目录路径
            String normalizedPath = normalizeDirectoryPath(directoryPath);

            // 在阿里云OSS中，目录是通过创建一个以斜杠结尾的空对象来实现的
            // 创建目录对象（空文件，大小为0）
            ossClient.putObject(bucketName, normalizedPath, new java.io.ByteArrayInputStream(new byte[0]));

            log.info("目录创建成功: {}/{}", bucketName, normalizedPath);
            return true;
        } catch (OSSException e) {
            log.error("OSS错误: {}", e.getErrorMessage());
            return false;
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

            // 检查Bucket是否存在
            if (!ossClient.doesBucketExist(bucketName)) {
                log.warn("Bucket不存在: {}", bucketName);
                return false;
            }

            // 列出目录下的所有对象（包括子目录）
            List<String> objectsToDelete = new ArrayList<>();
            String nextMarker = null;
            boolean isTruncated;

            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                        .withPrefix(normalizedPath)
                        .withMarker(nextMarker);

                com.aliyun.oss.model.ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);

                for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    objectsToDelete.add(objectSummary.getKey());
                }

                nextMarker = objectListing.getNextMarker();
                isTruncated = objectListing.isTruncated();

            } while (isTruncated);

            if (objectsToDelete.isEmpty()) {
                log.info("目录为空或不存在: {}/{}", bucketName, normalizedPath);
                return true;
            }

            // 批量删除目录下的所有对象（最多一次删除1000个对象）
            int batchSize = 1000;
            for (int i = 0; i < objectsToDelete.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, objectsToDelete.size());
                List<String> batch = objectsToDelete.subList(i, endIndex);

                DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
                        .withKeys(batch)
                        .withQuiet(true);

                ossClient.deleteObjects(deleteObjectsRequest);
            }

            log.info("目录删除成功: {}/{}，共删除{}个对象", bucketName, normalizedPath, objectsToDelete.size());
            return true;
        } catch (OSSException e) {
            log.error("OSS错误: {}", e.getErrorMessage());
            return false;
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
            if (!ossClient.doesBucketExist(bucketName)) {
                log.warn("Bucket不存在: {}", bucketName);
                return false;
            }

            // 列出旧目录下的所有对象
            List<String> objectsToCopy = new ArrayList<>();
            String nextMarker = null;
            boolean isTruncated;

            do {
                ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                        .withPrefix(normalizedOldPath)
                        .withMarker(nextMarker);

                com.aliyun.oss.model.ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);

                for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    objectsToCopy.add(objectSummary.getKey());
                }

                nextMarker = objectListing.getNextMarker();
                isTruncated = objectListing.isTruncated();

            } while (isTruncated);

            if (objectsToCopy.isEmpty()) {
                log.info("目录为空或不存在: {}/{}", bucketName, normalizedOldPath);
                return false;
            }

            // 复制所有对象到新路径并删除旧对象
            for (String oldKey : objectsToCopy) {
                String newKey = oldKey.replace(normalizedOldPath, normalizedNewPath);

                // 复制对象
                ossClient.copyObject(bucketName, oldKey, bucketName, newKey);

                // 删除旧对象
                ossClient.deleteObject(bucketName, oldKey);
            }

            log.info("目录重命名成功: {}/{} -> {}/{}", bucketName, normalizedOldPath, bucketName, normalizedNewPath);
            return true;
        } catch (OSSException e) {
            log.error("OSS错误: {}", e.getErrorMessage());
            return false;
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
            if (!ossClient.doesBucketExist(bucketName)) {
                log.warn("Bucket不存在: {}", bucketName);
                return false;
            }

            // 上传文件
            com.aliyun.oss.model.PutObjectRequest putObjectRequest =
                    new com.aliyun.oss.model.PutObjectRequest(bucketName, filePath, inputStream);

            if (contentType != null) {
                com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
                metadata.setContentType(contentType);
                putObjectRequest.setMetadata(metadata);
            }

            ossClient.putObject(putObjectRequest);

            log.info("文件上传成功: {}/{}", bucketName, filePath);
            return true;
        } catch (OSSException e) {
            log.error("OSS错误: {}", e.getErrorMessage());
            return false;
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
            if (!ossClient.doesBucketExist(bucketName)) {
                log.warn("Bucket不存在: {}", bucketName);
                return null;
            }

            // 检查文件是否存在
            if (!ossClient.doesObjectExist(bucketName, filePath)) {
                log.warn("文件不存在: {}/{}", bucketName, filePath);
                return null;
            }

            // 下载文件
            com.aliyun.oss.model.OSSObject ossObject = ossClient.getObject(bucketName, filePath);
            return ossObject.getObjectContent();

        } catch (OSSException e) {
            log.error("OSS错误: {}", e.getErrorMessage());
            return null;
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
            if (!ossClient.doesBucketExist(bucketName)) {
                log.warn("Bucket不存在: {}", bucketName);
                return false;
            }

            // 检查文件是否存在
            if (!ossClient.doesObjectExist(bucketName, filePath)) {
                log.warn("文件不存在: {}/{}", bucketName, filePath);
                return false;
            }

            // 删除文件
            ossClient.deleteObject(bucketName, filePath);

            log.info("文件删除成功: {}/{}", bucketName, filePath);
            return true;
        } catch (OSSException e) {
            log.error("OSS错误: {}", e.getErrorMessage());
            return false;
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
        Assert.isTrue(tempFile.exists(), "文件不存在");
        Assert.isTrue(tempFile.isFile(), "路径不是文件");

        try {
            String bucketName = getBucketName(bucketSuffix);

            // 检查Bucket是否存在，如果不存在则创建
            if (!ossClient.doesBucketExist(bucketName)) {
                ossClient.createBucket(bucketName);
                log.info("创建Bucket: {}", bucketName);
            }

            // 根据日期生成目录结构（格式：yyyy/MM/dd/）
            String datePath = formatDatePath(date);

            // 生成最终的文件路径
            String fileName = tempFile.getName();
            String filePath = datePath + fileName;

            // 上传文件
            try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
                com.aliyun.oss.model.ObjectMetadata metadata = new com.aliyun.oss.model.ObjectMetadata();
                metadata.setContentLength(tempFile.length());
                metadata.setContentType(getContentType(fileName));

                ossClient.putObject(
                        bucketName,
                        filePath,
                        fileInputStream,
                        metadata
                );
            }

            // 构建完整的存储ID（包含bucket名称和文件路径）
            String storageId = bucketName + "/" + filePath;

            log.info("文件按日期上传成功: {}, 文件大小: {} bytes", storageId, tempFile.length());
            return storageId;

        } catch (OSSException e) {
            log.error("OSS错误: {}", e.getErrorMessage());
            log.error("按日期上传文件失败: bucketSuffix={}, file={}, date={}",
                    bucketSuffix, tempFile.getName(), date, e);
            return null;
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
        } else if (lowerFileName.endsWith(".zip")) {
            return "application/zip";
        } else if (lowerFileName.endsWith(".rar")) {
            return "application/x-rar-compressed";
        } else if (lowerFileName.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerFileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerFileName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerFileName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lowerFileName.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (lowerFileName.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else {
            return "application/octet-stream";
        }
    }
}

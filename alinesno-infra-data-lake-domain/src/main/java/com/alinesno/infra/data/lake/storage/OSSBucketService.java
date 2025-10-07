package com.alinesno.infra.data.lake.storage;

import java.io.File;

/**
 * OSSBucketService
 */
public interface OSSBucketService {

    boolean createBucket(String suffix);

    boolean deleteBucket(String suffix);

    boolean bucketExists(String suffix);

    String getBucketName(String suffix);

    /**
     * 在指定Bucket中创建目录（支持多层级）
     * @param bucketSuffix Bucket名称的后缀
     * @param directoryPath 目录路径，支持多层级（如：folder1/folder2/subfolder/）
     * @return 创建成功返回true，否则返回false
     */
    boolean createDirectory(String bucketSuffix, String directoryPath);

    /**
     * 删除指定Bucket中的目录（支持多层级）
     * @param bucketSuffix Bucket名称的后缀
     * @param directoryPath 目录路径，支持多层级（如：folder1/folder2/subfolder/）
     * @return 删除成功返回true，否则返回false
     */
    boolean deleteDirectory(String bucketSuffix, String directoryPath);

    /**
     * 重命名指定Bucket中的目录
     * @param bucketSuffix Bucket名称的后缀
     * @param oldDirectoryPath 旧目录路径
     * @param newDirectoryPath 新目录路径
     * @return 重命名成功返回true，否则返回false
     */
    boolean renameDirectory(String bucketSuffix, String oldDirectoryPath, String newDirectoryPath);

    /**
     * 上传文件到指定Bucket
     * @param bucketSuffix Bucket名称的后缀
     * @param filePath 文件路径（包含文件名）
     * @param inputStream 文件输入流
     * @param contentType 文件内容类型
     * @return 上传成功返回true，否则返回false
     */
    boolean uploadFile(String bucketSuffix, String filePath, java.io.InputStream inputStream, String contentType);

    /**
     * 下载指定Bucket中的文件
     * @param bucketSuffix Bucket名称的后缀
     * @param filePath 文件路径（包含文件名）
     * @return 文件输入流，如果失败返回null
     */
    java.io.InputStream downloadFile(String bucketSuffix, String filePath);

    /**
     * 删除指定Bucket中的文件
     * @param bucketSuffix Bucket名称的后缀
     * @param filePath 文件路径（包含文件名）
     * @return 删除成功返回true，否则返回false
     */
    boolean deleteFile(String bucketSuffix, String filePath);

    /**
     * 上传文件到指定Bucket，并指定日期
     * @param tempFile
     * @param date
     * @return
     */
    String uploadFileByDate(String bucketName , File tempFile, String date);
}

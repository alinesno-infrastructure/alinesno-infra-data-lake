package com.alinesno.infra.data.lake.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.lake.entity.StorageFileEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 存储文件服务接口
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
public interface IStorageFileService extends IBaseService<StorageFileEntity> {

    /**
     * 创建存储桶
     *
     * @return
     */
    String createBucket(String bucketName);

    /**
     * 删除存储桶
     * @param bucketName
     */
    void deleteBucket(String bucketName);

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
     * @param file 上传的文件
     * @return 上传成功返回true，否则返回false
     */
    boolean uploadFile(String bucketSuffix, String filePath, MultipartFile file);

    /**
     * 下载指定Bucket中的文件
     * @param bucketSuffix Bucket名称的后缀
     * @param filePath 文件路径（包含文件名）
     * @param response HttpServletResponse
     */
    void downloadFile(String bucketSuffix, String filePath, HttpServletResponse response);

    /**
     * 删除指定Bucket中的文件
     * @param bucketSuffix Bucket名称的后缀
     * @param filePath 文件路径（包含文件名）
     * @return 删除成功返回true，否则返回false
     */
    boolean deleteFile(String bucketSuffix, String filePath);

    /**
     * 上传文件
     * @param tempFile
     * @param date
     * @return
     */
    String upload(File tempFile, String date);

}

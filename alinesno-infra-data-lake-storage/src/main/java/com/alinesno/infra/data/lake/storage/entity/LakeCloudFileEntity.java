package com.alinesno.infra.data.lake.storage.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Index;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 云盘文件实体类
 * 使用MyBatis-Plus和A-Table注解实现自动建表和CRUD操作
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("lake_cloud_file")
@Table(name = "lake_cloud_file" , comment = "数据湖存储非结构化文件") // A-Table注解，指定表名
public class LakeCloudFileEntity extends InfraBaseEntity {

    /**
     * 文件名称
     */
    @Column(name = "file_name", type = MySqlTypeConstant.VARCHAR, length = 255, comment = "文件名称")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", type = MySqlTypeConstant.BIGINT, comment = "文件大小（字节）")
    private Long fileSize;

    /**
     * 文件类型
     */
    @Column(name = "file_type", type = MySqlTypeConstant.VARCHAR, length = 100, comment = "文件类型")
    private String fileType;

    /**
     * 文件扩展名
     */
    @Column(name = "file_extension", type = MySqlTypeConstant.VARCHAR, length = 50, comment = "文件扩展名")
    private String fileExtension;

    /**
     * 文件存储路径
     */
    @Column(name = "file_storage_id", type = MySqlTypeConstant.VARCHAR, length = 32, comment = "文件云存储ID")
    private String fileStorageId;

    /**
     * 文件MD5值（用于去重）
     */
    @Column(name = "file_md5", type = MySqlTypeConstant.VARCHAR, length = 32, comment = "文件MD5值")
    private String fileMd5;

    /**
     * 父目录ID（0表示根目录）
     */
    @Index
    @Column(name = "parent_id", type = MySqlTypeConstant.BIGINT, comment = "父目录ID")
    private Long parentId;

    /**
     * 是否为目录（0-文件，1-目录）
     */
    @Index
    @Column(name = "is_directory", type = MySqlTypeConstant.INT, length = 1, comment = "是否为目录（0-文件，1-目录）")
    private Integer isDirectory;

    /**
     * 分享状态（0-私有，1-公开，2-加密分享）
     */
    @Column(name = "share_status", type = MySqlTypeConstant.INT, length = 1, defaultValue = "0", comment = "分享状态")
    private Integer shareStatus;

    /**
     * 分享密码
     */
    @Column(name = "share_password", type = MySqlTypeConstant.VARCHAR, length = 100, comment = "分享密码")
    private String sharePassword;

    /**
     * 分享过期时间
     */
    @Column(name = "share_expire_time", type = MySqlTypeConstant.DATETIME, comment = "分享过期时间")
    private LocalDateTime shareExpireTime;

}
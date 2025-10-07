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

/**
 * 下载记录实体（actable 注解，自动建表）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("download_record")
@Table(name = "download_record", comment = "下载记录表")
public class DownloadRecordEntity extends InfraBaseEntity {

    // 关联的 token id（如果通过 token 下载）
    @Index
    @Column(name = "token_id", type = MySqlTypeConstant.BIGINT, comment = "关联 token id")
    private Long tokenId;

    // 下载的 file id（必填）
    @Index
    @Column(name = "file_id", type = MySqlTypeConstant.BIGINT, comment = "文件ID")
    private Long fileId;

    // 对应的云存储 id（fileStorageId）
    @Column(name = "storage_id", type = MySqlTypeConstant.VARCHAR, length = 64, comment = "云存储ID")
    private String storageId;

    @Column(name = "file_name", type = MySqlTypeConstant.VARCHAR, length = 255, comment = "文件名称")
    private String fileName;

    @Column(name = "file_size", type = MySqlTypeConstant.BIGINT, comment = "文件大小（字节）")
    private Long fileSize;

    // 下载是否成功（0-失败，1-成功）
    @Column(name = "success", type = MySqlTypeConstant.INT, length = 1, defaultValue = "0", comment = "下载是否成功：0失败，1成功")
    private Integer success;

    @Column(name = "client_ip", type = MySqlTypeConstant.VARCHAR, length = 64, comment = "客户端 IP")
    private String clientIp;

    @Column(name = "user_agent", type = MySqlTypeConstant.VARCHAR, length = 512, comment = "User-Agent")
    private String userAgent;

    // 备注
    @Column(name = "remark", type = MySqlTypeConstant.VARCHAR, length = 512, comment = "备注")
    private String remark;

}
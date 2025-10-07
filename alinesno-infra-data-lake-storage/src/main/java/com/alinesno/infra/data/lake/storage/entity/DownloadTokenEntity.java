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

import java.util.Date;

/**
 * 下载 Token 实体（actable 注解，自动建表）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("download_token")
@Table(name = "download_token", comment = "下载 Token 表")
public class DownloadTokenEntity extends InfraBaseEntity {

    // 唯一 token 字符串（前端/客户端使用）
    @Index   // 为 token 建索引（如需唯一索引，可在数据库层补建）
    @Column(name = "token", type = MySqlTypeConstant.VARCHAR, length = 128, comment = "下载 token")
    private String token;

    /**
     * 目标类型：
     * 0 - 任意（Token 可下载任意文件/文件夹）
     * 1 - 指定文件（target_id 指向 fileId）
     * 2 - 指定文件夹（target_id 指向 folderId）
     */
    @Column(name = "target_type", type = MySqlTypeConstant.INT, length = 1, defaultValue = "0", comment = "目标类型：0任意/1文件/2文件夹")
    private Integer targetType;

    // 目标 id（fileId 或 folderId），当 target_type != 0 时使用
    @Index
    @Column(name = "target_id", type = MySqlTypeConstant.BIGINT, comment = "目标ID（文件或文件夹）")
    private Long targetId;

    // 最大可下载次数，0 表示不限制
    @Column(name = "max_uses", type = MySqlTypeConstant.INT, defaultValue = "0", comment = "最大可下载次数，0不限制")
    private Integer maxUses;

    // 已使用次数
    @Column(name = "used_count", type = MySqlTypeConstant.INT, defaultValue = "0", comment = "已使用次数")
    private Integer usedCount;

    // 过期时间，null 表示不过期
    @Column(name = "expire_time", type = MySqlTypeConstant.DATETIME, comment = "过期时间")
    private Date expireTime;

    // 状态：0-有效，1-已禁用/撤销，2-已用尽
    @Index
    @Column(name = "status", type = MySqlTypeConstant.INT, length = 1, defaultValue = "0", comment = "状态：0有效，1禁用，2已用尽")
    private Integer status;

    // 创建者（可选）
    @Column(name = "creator_id", type = MySqlTypeConstant.BIGINT, comment = "创建者ID")
    private Long creatorId;

    // 备注
    @Column(name = "remark", type = MySqlTypeConstant.VARCHAR, length = 512, comment = "备注")
    private String remark;

}
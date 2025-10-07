package com.alinesno.infra.data.lake.storage.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.lake.storage.entity.DownloadTokenEntity;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DownloadTokenService extends IBaseService<DownloadTokenEntity> {

    /**
     * 校验 token 是否有效并尝试消费一次使用（若 maxUses>0）。
     * 若有效返回对应的 DownloadTokenEntity；若无效抛出异常。
     *
     * 注意：此方法应保证并发安全（竞争消费时只允许一次成功增加 usedCount）。
     *
     * @param tokenStr token 字符串
     * @param targetType 请求目标类型（0/1/2），用于二次校验（可传 null 表示不校验）
     * @param targetId 请求目标 id（fileId 或 folderId），用于校验 token 是否允许访问该目标
     * @return DownloadTokenEntity 若成功
     * @throws IllegalArgumentException 或自定义异常 表示 token 无效/过期/次数用尽/权限不足
     */
    DownloadTokenEntity validateAndConsume(String tokenStr, Integer targetType, Long targetId);
}
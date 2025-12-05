package com.alinesno.infra.data.lake.storage.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.lake.storage.entity.DownloadTokenEntity;
import com.alinesno.infra.data.lake.storage.mapper.DownloadTokenMapper;
import com.alinesno.infra.data.lake.storage.service.DownloadTokenService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DownloadTokenServiceImpl extends IBaseServiceImpl<DownloadTokenEntity,DownloadTokenMapper>  implements DownloadTokenService {

    @Override
    public DownloadTokenEntity validateAndConsume(String tokenStr, Integer targetType, Long targetId) {
        if (tokenStr == null || tokenStr.trim().isEmpty()) {
            throw new IllegalArgumentException("下载 token 为空");
        }

        DownloadTokenEntity token = lambdaQuery().eq(DownloadTokenEntity::getToken, tokenStr).one();
        if (token == null) {
            throw new IllegalArgumentException("下载 token 无效");
        }

        // 状态校验
        if (token.getStatus() == null || token.getStatus() != 0) {
            throw new IllegalArgumentException("下载 token 不可用");
        }

        // 过期校验
        Date now = new Date();
        if (token.getExpireTime() != null && token.getExpireTime().before(now)) {
            throw new IllegalArgumentException("下载 token 已过期");
        }

        // 目标校验（若 token 限定了特定目标）
        if (token.getTargetType() != null && token.getTargetType() != 0) {
            if (targetType == null || targetId == null) {
                throw new IllegalArgumentException("缺少目标信息，无法校验 token 权限");
            }
            if (!token.getTargetType().equals(targetType) || !token.getTargetId().equals(targetId)) {
                throw new IllegalArgumentException("下载 token 无权访问该资源");
            }
        }

        // 使用次数限制与原子消费
        Integer maxUses = token.getMaxUses() == null ? 0 : token.getMaxUses();
        if (maxUses != null && maxUses > 0) {
            // 仅当 used_count < maxUses 时才增加 used_count
            UpdateWrapper<DownloadTokenEntity> uw = new UpdateWrapper<>();
            uw.eq("id", token.getId())
              .lt("used_count", maxUses)
              .setSql("used_count = used_count + 1");

            boolean updated = update(uw);
            if (!updated) {
                // 更新失败表示 used_count 已经 >= maxUses
                // 可以把状态设置为已用尽
                token.setStatus(2);
                updateById(token);
                throw new IllegalArgumentException("下载 token 次数已用尽");
            } else {
                // 手动增加内存对象的 usedCount（注意并发时可能不是精确值，但用于返回）
                Integer used = token.getUsedCount() == null ? 0 : token.getUsedCount();
                token.setUsedCount(used + 1);
            }
        }

        return token;
    }
}
package com.alinesno.infra.data.lake.storage.service.impl;

import com.alinesno.infra.data.lake.storage.entity.LakeCloudFileEntity;
import com.alinesno.infra.data.lake.storage.mapper.LakeCloudFileMapper;
import com.alinesno.infra.data.lake.storage.service.ILakeCloudDownloadFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LakeCloudDownloadFileServiceImpl implements ILakeCloudDownloadFileService {

    @Autowired
    private LakeCloudFileMapper mapper;

    @Override
    public LakeCloudFileEntity getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<LakeCloudFileEntity> listFilesUnderFolder(Long folderId) {
        // BFS 获取该目录及子目录下所有文件
        List<LakeCloudFileEntity> result = new ArrayList<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(folderId);
        while (!queue.isEmpty()) {
            Long pid = queue.poll();
            LambdaQueryWrapper<LakeCloudFileEntity> qw = new LambdaQueryWrapper<>();
            qw.eq(LakeCloudFileEntity::getParentId, pid);
            List<LakeCloudFileEntity> children = mapper.selectList(qw);
            if (children == null) continue;
            for (LakeCloudFileEntity c : children) {
                if (c.getIsDirectory() != null && c.getIsDirectory() == 1) {
                    queue.add(c.getId());
                } else {
                    result.add(c);
                }
            }
        }
        return result;
    }

    @Override
    public List<LakeCloudFileEntity> listFilesUnderFolderByDate(Long folderId, Date startTime, Date endTime) {
        // 1. 递归收集所有目录 id（包括自身）
        Set<Long> folderIds = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(folderId);
        while (!queue.isEmpty()) {
            Long pid = queue.poll();
            folderIds.add(pid);
            LambdaQueryWrapper<LakeCloudFileEntity> qw = new LambdaQueryWrapper<>();
            qw.eq(LakeCloudFileEntity::getParentId, pid)
                    .eq(LakeCloudFileEntity::getIsDirectory, 1);
            List<LakeCloudFileEntity> childrenFolders = mapper.selectList(qw);
            if (childrenFolders != null) {
                for (LakeCloudFileEntity cf : childrenFolders) {
                    if (cf.getId() != null && !folderIds.contains(cf.getId())) {
                        queue.add(cf.getId());
                    }
                }
            }
        }

        if (folderIds.isEmpty()) return Collections.emptyList();

        // 2. 将时间转换为 Date
        Date startDate = (startTime == null) ? null : Date.from(startTime.toInstant());
        Date endDate = (endTime == null) ? null : Date.from(endTime.toInstant());

        // 3. 构造查询，仅查询 is_directory = 0 的文件且 parent_id 在 folderIds 中，并按时间范围过滤
        LambdaQueryWrapper<LakeCloudFileEntity> fileQw = new LambdaQueryWrapper<>();
        fileQw.in(LakeCloudFileEntity::getParentId, folderIds)
                .eq(LakeCloudFileEntity::getIsDirectory, 0);

        if (startDate != null && endDate != null) {
            fileQw.between(LakeCloudFileEntity::getAddTime, startDate, endDate);
        } else if (startDate != null) {
            fileQw.ge(LakeCloudFileEntity::getAddTime, startDate);
        } else if (endDate != null) {
            fileQw.le(LakeCloudFileEntity::getAddTime, endDate);
        }

        return mapper.selectList(fileQw);
    }
}
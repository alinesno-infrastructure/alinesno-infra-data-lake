package com.alinesno.infra.data.lake.storage.provider;

import com.alinesno.infra.data.lake.adapter.service.CloudStorageConsumer;
import com.alinesno.infra.data.lake.storage.entity.LakeCloudFileEntity;
import com.alinesno.infra.data.lake.storage.interceptor.DownloadTokenRequired;
import com.alinesno.infra.data.lake.storage.service.ILakeCloudDownloadFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * LakeStorageDownloadController
 */
@Slf4j
@DownloadTokenRequired
@RestController
@RequestMapping("/api/lake/storage/download")
public class LakeStorageDownloadController {

    @Autowired
    private ILakeCloudDownloadFileService lakeCloudFileService;

    @Autowired
    private CloudStorageConsumer cloudStorageConsumer;

    /**
     * 根据 id 下载：如果是文件 => 将该文件打包成 zip 并返回；
     * 如果是文件夹 => 将该文件夹下所有文件打包并返回。
     *
     * GET /api/lake/storage/download/{id}
     */
    @GetMapping("/{id}")
    public void downloadById(@PathVariable Long id, HttpServletResponse response) throws IOException {
        LakeCloudFileEntity entity = lakeCloudFileService.getById(id);
        if (entity == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "资源不存在");
            return;
        }

        String zipName = safeZipName(entity.getFileName(), "download");
        setZipResponseHeader(response, zipName);

        Set<String> usedEntryNames = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {
            if (entity.getIsDirectory() != null && entity.getIsDirectory() == 1) {
                // 获取目录下所有文件
                List<LakeCloudFileEntity> files = lakeCloudFileService.listFilesUnderFolder(id);
                for (LakeCloudFileEntity f : files) {
                    if (f.getIsDirectory() != null && f.getIsDirectory() == 1) continue;
                    addFileToZip(zos, f, usedEntryNames);
                }
            } else {
                // 单文件
                addFileToZip(zos, entity, usedEntryNames);
            }
            zos.finish();
        } catch (Exception e) {
            log.error("打包下载失败 id={}", id, e);
            // response 已经流出，无法再次设置错误状态
        }
    }

    /**
     * 按日期过滤下载文件夹下的文件。
     * 支持参数：
     * - date = yyyy-MM-dd（只下载该日的文件）
     * - startDate / endDate = yyyy-MM-dd（下载区间，包含两端）
     *
     * GET /api/lake/storage/download/{id}/byDate?date=2025-10-07
     */
    @GetMapping("/{id}/byDate")
    public void downloadByIdWithDate(
            @PathVariable Long id,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {

        LakeCloudFileEntity entity = lakeCloudFileService.getById(id);
        if (entity == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "资源不存在");
            return;
        }

        // 如果不是目录，直接按文件下载（忽略日期）
        if (entity.getIsDirectory() == null || entity.getIsDirectory() != 1) {
            downloadById(id, response);
            return;
        }

        LocalDate start = null;
        LocalDate end = null;
        try {
            if (date != null && !date.isEmpty()) {
                start = LocalDate.parse(date);
                end = start;
            } else {
                if (startDate != null && !startDate.isEmpty()) start = LocalDate.parse(startDate);
                if (endDate != null && !endDate.isEmpty()) end = LocalDate.parse(endDate);
            }
        } catch (DateTimeParseException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "日期格式错误，需为 yyyy-MM-dd");
            return;
        }

        LocalDateTime startTime = (start != null) ? start.atStartOfDay() : null;
        LocalDateTime endTime = (end != null) ? end.plusDays(1).atStartOfDay().minusNanos(1) : null;

        Date startDateObj = (startTime == null) ? null : Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDateObj = (endTime == null) ? null : Date.from(endTime.atZone(ZoneId.systemDefault()).toInstant());

        String zipName = safeZipName(entity.getFileName(), "download");
        setZipResponseHeader(response, zipName);

        Set<String> usedEntryNames = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {
            List<LakeCloudFileEntity> files = lakeCloudFileService.listFilesUnderFolderByDate(id, startDateObj, endDateObj);
            for (LakeCloudFileEntity f : files) {
                if (f.getIsDirectory() != null && f.getIsDirectory() == 1) continue;
                addFileToZip(zos, f, usedEntryNames);
            }
            zos.finish();
        } catch (Exception e) {
            log.error("按日期打包下载失败 id={} start={} end={}", id, startDateObj, endDateObj, e);
        }
    }

    private String safeZipName(String srcName, String defaultName) throws IOException {
        String name = (srcName == null || srcName.trim().isEmpty()) ? defaultName : srcName;
        if (!name.toLowerCase().endsWith(".zip")) name = name + ".zip";
        return name;
    }

    private void setZipResponseHeader(HttpServletResponse response, String zipName) throws IOException {
        response.setContentType("application/zip");
        String encoded = URLEncoder.encode(zipName, "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }

    /**
     * 将单个文件加入 zip，确保 entry 名称唯一，捕获重复 entry 异常并记录后跳过。
     */
    private void addFileToZip(ZipOutputStream zos, LakeCloudFileEntity fileEntity, Set<String> usedNames) {
        if (fileEntity == null) return;

        String rawName = fileEntity.getFileName();
        if (rawName == null || rawName.trim().isEmpty()) rawName = fileEntity.getFileStorageId();
        rawName = rawName.replace("\\", "/"); // 统一路径分隔符

        // 这里只保留文件名（不包含全路径），若要保留目录结构请在 service 返回相对路径并使用该路径
        String entryName = rawName;
        if (entryName.contains("/")) {
            entryName = entryName.substring(entryName.lastIndexOf('/') + 1);
        }

        entryName = makeUniqueEntryName(entryName, usedNames, fileEntity.getFileStorageId());

        try {
            byte[] bytes = cloudStorageConsumer.download(fileEntity.getFileStorageId(), null);
            if (bytes == null || bytes.length == 0) {
                log.warn("文件为空或未获取到 bytes storageId={}", fileEntity.getFileStorageId());
                return;
            }
            ZipEntry entry = new ZipEntry(entryName);
            entry.setSize(bytes.length);
            zos.putNextEntry(entry);
            zos.write(bytes);
            zos.closeEntry();
            log.debug("已添加到 zip: {}", entryName);
        } catch (java.util.zip.ZipException ze) {
            log.warn("添加文件到 zip 失败（ZipException） storageId={} name={}, message={}. 跳过.",
                    fileEntity.getFileStorageId(), entryName, ze.getMessage());
        } catch (Exception e) {
            log.error("添加文件到 zip 失败 storageId={} name={}", fileEntity.getFileStorageId(), entryName, e);
        }
    }

    /**
     * 基于文件名 + storageId 保证 zip entry 名称唯一。
     */
    private String makeUniqueEntryName(String entryName, Set<String> usedNames, String storageId) {
        if (!usedNames.contains(entryName)) {
            usedNames.add(entryName);
            return entryName;
        }
        int dotIndex = entryName.lastIndexOf('.');
        String base;
        String ext;
        if (dotIndex > 0 && dotIndex < entryName.length() - 1) {
            base = entryName.substring(0, dotIndex);
            ext = entryName.substring(dotIndex); // 包含点
        } else {
            base = entryName;
            ext = "";
        }
        String candidate = base + "_" + storageId + ext;
        int idx = 1;
        while (usedNames.contains(candidate)) {
            candidate = base + "_" + storageId + "_" + idx + ext;
            idx++;
        }
        usedNames.add(candidate);
        return candidate;
    }
}
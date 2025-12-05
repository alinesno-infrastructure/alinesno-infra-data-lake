package com.alinesno.infra.data.lake.storage.interceptor;

import com.alinesno.infra.data.lake.storage.entity.DownloadRecordEntity;
import com.alinesno.infra.data.lake.storage.entity.DownloadTokenEntity;
import com.alinesno.infra.data.lake.storage.entity.LakeCloudFileEntity;
import com.alinesno.infra.data.lake.storage.service.DownloadRecordService;
import com.alinesno.infra.data.lake.storage.service.DownloadTokenService;
import com.alinesno.infra.data.lake.storage.service.ILakeCloudDownloadFileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Date;
import java.util.Map;

/**
 * 拦截被 @DownloadTokenRequired 注解的方法，校验 token。
 * 校验通过后在请求属性中放入 "DOWNLOAD_TOKEN" -> DownloadTokenEntity，供后续使用。
 * 请求完成后记录下载记录（无论成功或失败都会记录一条）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadTokenInterceptor implements HandlerInterceptor {

    private final DownloadTokenService downloadTokenService;
    private final DownloadRecordService downloadRecordService;
    private final ILakeCloudDownloadFileService lakeCloudFileService; // 用于获取 file 元数据

    public static final String ATTR_DOWNLOAD_TOKEN = "DOWNLOAD_TOKEN";
    public static final String ATTR_DOWNLOAD_FILE_ID = "DOWNLOAD_FILE_ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod hm = (HandlerMethod) handler;
        DownloadTokenRequired methodAnno = hm.getMethodAnnotation(DownloadTokenRequired.class);
        DownloadTokenRequired classAnno = hm.getBeanType().getAnnotation(DownloadTokenRequired.class);
        DownloadTokenRequired anno = methodAnno != null ? methodAnno : classAnno;
        if (anno == null) {
            return true; // 不需要 token 校验
        }

        // 从 header 或参数获取 token
        String tokenStr = request.getHeader("Download-Token");
        if (tokenStr == null || tokenStr.isEmpty()) {
            tokenStr = request.getParameter("token");
        }

        // 获取 path variable id（如 /{id}）
        Long fileId = null;
        @SuppressWarnings("unchecked")
        Map<String, String> uriVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVars != null) {
            String idStr = uriVars.get("id");
            if (idStr != null) {
                try {
                    fileId = Long.valueOf(idStr);
                } catch (NumberFormatException ignored) {}
            }
        }

        try {
            Integer targetType = anno.targetType(); // 期望目标类型
            // 若 controller 是针对单文件下载，targetType 通常为 1；但我们也允许 token 内限制来决定最终是否允许。
            DownloadTokenEntity token = downloadTokenService.validateAndConsume(tokenStr, targetType, fileId);
            // 保存 token 与 fileId 到 request，便于后续记录使用
            request.setAttribute(ATTR_DOWNLOAD_TOKEN, token);
            if (fileId != null) request.setAttribute(ATTR_DOWNLOAD_FILE_ID, fileId);
            return true;
        } catch (IllegalArgumentException ex) {
            log.warn("下载 token 校验失败: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"" + ex.getMessage() + "\"}");
            return false;
        } catch (Exception ex) {
            log.error("下载 token 校验异常", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"服务异常\"}");
            return false;
        }
    }

    /**
     * 请求完成后记录下载记录
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Object tokenObj = request.getAttribute(ATTR_DOWNLOAD_TOKEN);
        Object fidObj = request.getAttribute(ATTR_DOWNLOAD_FILE_ID);
        if (tokenObj == null || fidObj == null) {
            return;
        }
        DownloadTokenEntity token = (DownloadTokenEntity) tokenObj;
        Long fileId = (Long) fidObj;

        DownloadRecordEntity record = new DownloadRecordEntity();
        record.setTokenId(token.getId());
        record.setFileId(fileId);

        try {
            LakeCloudFileEntity file = lakeCloudFileService.getById(fileId);
            if (file != null) {
                record.setStorageId(file.getFileStorageId());
                record.setFileName(file.getFileName());
                record.setFileSize(file.getFileSize());
            }
        } catch (Exception e) {
            log.warn("获取文件元信息异常 fileId={}", fileId, e);
        }

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        record.setClientIp(clientIp);
        record.setUserAgent(request.getHeader("User-Agent"));
        record.setAddTime(new Date());
        record.setSuccess((ex == null && response.getStatus() >= 200 && response.getStatus() < 300) ? 1 : 0);

        try {
            downloadRecordService.save(record);
        } catch (Exception e) {
            log.error("保存下载记录失败", e);
        }
    }
}
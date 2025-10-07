package com.alinesno.infra.data.lake.storage.interceptor;

import java.lang.annotation.*;

/**
 * 标注需要下载 token 校验的方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DownloadTokenRequired {
    /**
     * 期望的目标类型（0 - 任意，1 - 文件，2 - 文件夹）
     * 默认使用 0 （任意）——拦截器仍会检查 token 是否限制到特定目标
     */
    int targetType() default 0;
}
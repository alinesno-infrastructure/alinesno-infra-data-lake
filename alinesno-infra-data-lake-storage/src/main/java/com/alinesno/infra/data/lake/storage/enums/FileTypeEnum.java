package com.alinesno.infra.data.lake.storage.enums;

import lombok.Getter;

/**
 * 文件类型枚举
 * 用于标识是文件还是目录
 */
@Getter
public enum FileTypeEnum {
    
    /**
     * 文件
     */
    FILE(0, "文件"),
    
    /**
     * 目录
     */
    DIRECTORY(1, "目录");

    private final Integer code;
    private final String description;

    FileTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static FileTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FileTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为文件
     */
    public static boolean isFile(Integer code) {
        return FILE.getCode().equals(code);
    }

    /**
     * 判断是否为目录
     */
    public static boolean isDirectory(Integer code) {
        return DIRECTORY.getCode().equals(code);
    }

    /**
     * 验证code是否有效
     */
    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        for (FileTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
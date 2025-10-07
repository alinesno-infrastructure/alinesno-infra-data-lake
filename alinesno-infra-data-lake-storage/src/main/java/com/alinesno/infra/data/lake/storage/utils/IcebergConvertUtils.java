package com.alinesno.infra.data.lake.storage.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
public class IcebergConvertUtils {

    /**
     * 将输入值转换为Iceberg所需的类型
     */
    public static Object convertValueToIcebergType(Object value, org.apache.iceberg.types.Type icebergType) {
        if (value == null) {
            return null;
        }

        // 如果已经是目标类型，直接返回
        if (isCompatibleType(value, icebergType)) {
            return value;
        }

        try {
            switch (icebergType.typeId()) {
                case TIMESTAMP:
                    // 处理时间戳类型
                    return convertToLocalDateTime(value);

                case DATE:
                    // 处理日期类型
                    return convertToLocalDate(value);

                case TIME:
                    // 处理时间类型
                    return convertToLocalTime(value);

                case INTEGER:
                    if (value instanceof String) {
                        return Integer.parseInt((String) value);
                    } else if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                    break;

                case LONG:
                    if (value instanceof String) {
                        return Long.parseLong((String) value);
                    } else if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                    break;

                case FLOAT:
                    if (value instanceof String) {
                        return Float.parseFloat((String) value);
                    } else if (value instanceof Number) {
                        return ((Number) value).floatValue();
                    }
                    break;

                case DOUBLE:
                    if (value instanceof String) {
                        return Double.parseDouble((String) value);
                    } else if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                    break;

                case BOOLEAN:
                    if (value instanceof String) {
                        return Boolean.parseBoolean((String) value);
                    } else if (value instanceof Number) {
                        return ((Number) value).intValue() != 0;
                    }
                    break;

                case DECIMAL:
                    if (value instanceof String) {
                        return new BigDecimal((String) value);
                    } else if (value instanceof Number) {
                        return BigDecimal.valueOf(((Number) value).doubleValue());
                    }
                    break;

                default:
                    // 对于字符串和其他类型，直接toString
                    return value.toString();
            }
        } catch (Exception e) {
            log.warn("类型转换失败: value={}, targetType={}, error={}",
                    value, icebergType, e.getMessage());
            throw new IllegalArgumentException("无法将值 " + value + " 转换为类型 " + icebergType);
        }

        // 默认情况下尝试字符串转换
        return value.toString();
    }

    /**
     * 检查值是否与Iceberg类型兼容
     */
    private static boolean isCompatibleType(Object value, org.apache.iceberg.types.Type icebergType) {
        return switch (icebergType.typeId()) {
            case TIMESTAMP -> value instanceof LocalDateTime;
            case DATE -> value instanceof LocalDate;
            case TIME -> value instanceof LocalTime;
            case INTEGER -> value instanceof Integer;
            case LONG -> value instanceof Long;
            case FLOAT -> value instanceof Float;
            case DOUBLE -> value instanceof Double;
            case BOOLEAN -> value instanceof Boolean;
            case DECIMAL -> value instanceof BigDecimal;
            case STRING -> value instanceof String;
            default -> true; // 对于其他类型，放宽检查
        };
    }

    /**
     * 转换为LocalDateTime（支持多种时间格式）
     */
    private static LocalDateTime convertToLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof String) {
            String strValue = (String) value;

            // 移除可能的多余空格
            strValue = strValue.trim();

            try {
                // 首先尝试ISO格式（带T的）
                return LocalDateTime.parse(strValue);
            } catch (DateTimeParseException e1) {
                try {
                    // 尝试空格分隔的格式（yyyy-MM-dd HH:mm:ss）
                    return LocalDateTime.parse(strValue,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException e2) {
                    try {
                        // 尝试没有秒数的格式（yyyy-MM-dd HH:mm）
                        return LocalDateTime.parse(strValue,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    } catch (DateTimeParseException e3) {
                        try {
                            // 尝试只有日期的格式（自动补全时间为00:00:00）
                            if (strValue.length() == 10) { // yyyy-MM-dd
                                LocalDate date = LocalDate.parse(strValue);
                                return date.atStartOfDay();
                            }
                        } catch (DateTimeParseException e4) {
                            // 继续尝试其他格式
                        }

                        try {
                            // 尝试中文常见格式（yyyy年MM月dd日 HH:mm:ss）
                            if (strValue.contains("年") && strValue.contains("月") && strValue.contains("日")) {
                                return LocalDateTime.parse(strValue,
                                        DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));
                            }
                        } catch (DateTimeParseException e5) {
                            // 继续尝试其他格式
                        }

                        try {
                            // 尝试带毫秒的格式
                            if (strValue.contains(".")) {
                                return LocalDateTime.parse(strValue,
                                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                            }
                        } catch (DateTimeParseException e6) {
                            // 继续尝试其他格式
                        }

                        // 最后尝试时间戳（毫秒或秒）
                        try {
                            long timestamp = Long.parseLong(strValue);
                            // 判断是秒还是毫秒（通常13位是毫秒，10位是秒）
                            if (strValue.length() == 10) { // 秒
                                timestamp *= 1000;
                            }
                            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                        } catch (NumberFormatException e7) {
                            throw new IllegalArgumentException("无法解析时间戳，支持的格式: yyyy-MM-dd HH:mm:ss, yyyy-MM-ddTHH:mm:ss, 时间戳等。实际值: " + strValue);
                        }
                    }
                }
            }
        } else if (value instanceof Long) {
            // 时间戳毫秒数
            return LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) value), ZoneId.systemDefault());
        } else if (value instanceof Integer) {
            // 时间戳秒数
            long timestamp = ((Integer) value).longValue() * 1000;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        } else if (value instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), ZoneId.systemDefault());
        }

        throw new IllegalArgumentException("不支持的时间戳类型: " + (value != null ? value.getClass().getName() : "null"));
    }

    /**
     * 转换为LocalDate
     */
    public static LocalDate convertToLocalDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        } else if (value instanceof String) {
            return LocalDate.parse((String) value);
        } else if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalDate();
        } else if (value instanceof Long) {
            return Instant.ofEpochMilli((Long) value).atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        throw new IllegalArgumentException("不支持的日期类型: " + value.getClass().getName());
    }

    /**
     * 转换为LocalTime
     */
    public static LocalTime convertToLocalTime(Object value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        } else if (value instanceof String) {
            return LocalTime.parse((String) value);
        } else if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalTime();
        }
        throw new IllegalArgumentException("不支持的时间类型: " + value.getClass().getName());
    }

}

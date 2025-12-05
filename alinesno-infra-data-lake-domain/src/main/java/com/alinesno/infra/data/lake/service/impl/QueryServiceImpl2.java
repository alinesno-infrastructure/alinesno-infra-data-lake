//package com.alinesno.infra.data.lake.service.impl;
//
//import com.alinesno.infra.data.lake.api.websql.QueryRequest;
//import com.alinesno.infra.data.lake.api.websql.QueryResponse;
//import com.alinesno.infra.data.lake.service.IQueryService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.iceberg.*;
//import org.apache.iceberg.catalog.Catalog;
//import org.apache.iceberg.catalog.Namespace;
//import org.apache.iceberg.catalog.TableIdentifier;
//import org.apache.iceberg.io.CloseableIterable;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.lang.reflect.Method;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * Iceberg SQL查询服务实现（说明：仅实现基于元数据的轻量级功能：COUNT 支持 + explain）
// */
//@Slf4j
//@Service
//public class QueryServiceImpl2 implements IQueryService {
//
//    // 注入 Iceberg Catalog（在 IcebergConfig 中定义的 bean 名称）
//    @Resource
//    @Qualifier("jdbcIcebergCatalog")
//    private Catalog icebergCatalog;
//
//    private static final Pattern FROM_PATTERN = Pattern.compile("(?i)\\bfrom\\s+([`\\w.\\-]+)");
//
//    @Override
//    public QueryResponse executeQuery(QueryRequest request) {
//        QueryResponse resp = new QueryResponse();
//        try {
//            String sql = request == null ? null : request.getSql();
//            if (sql == null || sql.trim().isEmpty()) {
//                resp.setError("sql is empty");
//                resp.setTotal(-1L);
//                return resp;
//            }
//
//            String lower = sql.trim().toLowerCase(Locale.ROOT);
//
//            // 目前仅支持 count(*) 类的快速统计（基于 Iceberg DataFile.recordCount）
//            if (lower.matches("(?s).*\\bcount\\s*\\(.*\\).*")) {
//                String tableName = extractTableName(sql);
//                if (tableName == null) {
//                    resp.setError("cannot parse table name from sql for count query");
//                    resp.setTotal(-1L);
//                    return resp;
//                }
//
//                Table table = loadTableSafely(tableName);
//                if (table == null) {
//                    resp.setError("table not found: " + tableName);
//                    resp.setTotal(-1L);
//                    return resp;
//                }
//
//                // 使用 TableScan 的 planFiles 来累加 DataFile.recordCount（注意：recordCount 依赖写入时是否记录）
//                long totalRecords = 0L;
//                long totalFiles = 0L;
//                long totalBytes = 0L;
//                try (CloseableIterable<FileScanTask> tasks = table.newScan().planFiles()) {
//                    for (FileScanTask task : tasks) {
//                        DataFile file = task.file();
//                        if (file != null) {
//                            totalFiles++;
//                            // recordCount 可能为 null/-1，做保护
//                            try {
//                                long rc = file.recordCount();
//                                if (rc >= 0) {
//                                    totalRecords += rc;
//                                }
//                            } catch (Throwable t) {
//                                // 某些版本/场景下 recordCount 可能不可用，跳过
//                                log.debug("failed to read recordCount for file {}, reason: {}", file.path(), t.getMessage());
//                            }
//                            try {
//                                long fs = file.fileSizeInBytes();
//                                if (fs >= 0) {
//                                    totalBytes += fs;
//                                }
//                            } catch (Throwable t) {
//                                log.debug("failed to read fileSize for file {}, reason: {}", file.path(), t.getMessage());
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    log.warn("failed to plan files for table {}: {}", tableName, e.getMessage(), e);
//                    resp.setError("failed to read table file list: " + e.getMessage());
//                    resp.setTotal(-1L);
//                    return resp;
//                }
//
//                // 构建响应：一行 count 值
//                Map<String, Object> row = new HashMap<>();
//                row.put("count", totalRecords);
//                resp.setColumns(Collections.singletonList("count"));
//                resp.setRows(Collections.singletonList(row));
//                resp.setTotal(totalRecords);
//                resp.setError(null);
//                return resp;
//            } else {
//                // 不支持的 SQL 类型（提示）
//                resp.setError("Currently only COUNT(*) style queries are supported by this lightweight service. " +
//                        "For full SELECT support please use a SQL engine (Spark/Trino/Flink) or call explainSql to inspect the table.");
//                resp.setTotal(-1L);
//                return resp;
//            }
//
//        } catch (Exception ex) {
//            log.error("executeQuery error: {}", ex.getMessage(), ex);
//            resp.setError("executeQuery error: " + ex.getMessage());
//            resp.setTotal(-1L);
//            return resp;
//        }
//    }
//
//    @Override
//    public String explainSql(String sql, Object tableContext) {
//        if (sql == null || sql.trim().isEmpty()) {
//            return "sql is empty";
//        }
//        try {
//            String tableName = extractTableName(sql);
//            if (tableName == null) {
//                return "Cannot parse table name from SQL. Please provide a SQL like: SELECT ... FROM db.table";
//            }
//
//            Table table = loadTableSafely(tableName);
//            if (table == null) {
//                return "Table not found: " + tableName;
//            }
//
//            StringBuilder sb = new StringBuilder();
//            sb.append("Table: ").append(tableName).append("\n");
////            sb.append("Identifier: ").append(table.identifier()).append("\n");
//
//            String idStr = null;
//            try {
//                // 先尝试 identifier()
//                Method m = Table.class.getMethod("identifier");
//                Object idObj = m.invoke(table);
//                idStr = idObj == null ? null : String.valueOf(idObj);
//            } catch (NoSuchMethodException nsme) {
//                try {
//                    // 回退到 name()
//                    Method m2 = Table.class.getMethod("name");
//                    Object idObj = m2.invoke(table);
//                    idStr = idObj == null ? null : String.valueOf(idObj);
//                } catch (NoSuchMethodException nsme2) {
//                    // 两个方法都不存在，使用 toString 作为最后手段
//                    idStr = table.toString();
//                } catch (Throwable t2) {
//                    idStr = table.toString();
//                }
//            } catch (Throwable t) {
//                idStr = table.toString();
//            }
//            sb.append("Identifier/Name: ").append(idStr).append("\n");
//
//            // Schema
//            Schema schema = table.schema();
//            sb.append("Schema:\n");
//            schema.columns().forEach(c -> sb.append("  - ").append(c.name()).append(" : ").append(c.type()).append("\n"));
//
//            // Partition spec
//            try {
//                sb.append("Partition spec: ").append(table.spec().toString()).append("\n");
//            } catch (Throwable t) {
//                sb.append("Partition spec: <unavailable: ").append(t.getMessage()).append(">\n");
//            }
//
//            // Snapshot info
//            try {
//                Snapshot snap = table.currentSnapshot();
//                if (snap != null) {
//                    sb.append("Current snapshot id: ").append(snap.snapshotId()).append("\n");
//                    sb.append("  sequence number: ").append(snap.sequenceNumber()).append("\n");
//                    sb.append("  manifest list: ").append(snap.manifestListLocation()).append("\n");
//                    sb.append("  summary:\n");
//                    Map<String, String> summary = snap.summary();
//                    if (summary != null) {
//                        summary.forEach((k, v) -> sb.append("    ").append(k).append(" = ").append(v).append("\n"));
//                    }
//                } else {
//                    sb.append("No current snapshot\n");
//                }
//            } catch (Throwable t) {
//                sb.append("Snapshot info: <unavailable: ").append(t.getMessage()).append(">\n");
//            }
//
//            // Files / records / bytes summary (based on manifest planning)
//            long totalFiles = 0L;
//            long totalRecords = 0L;
//            long totalBytes = 0L;
//            try (CloseableIterable<FileScanTask> tasks = table.newScan().planFiles()) {
//                for (FileScanTask task : tasks) {
//                    DataFile f = task.file();
//                    if (f != null) {
//                        totalFiles++;
//                        try {
//                            long rc = f.recordCount();
//                            if (rc > 0) totalRecords += rc;
//                        } catch (Throwable ignore) {}
//                        try {
//                            long fs = f.fileSizeInBytes();
//                            if (fs > 0) totalBytes += fs;
//                        } catch (Throwable ignore) {}
//                    }
//                }
//                sb.append("Data files (planned): ").append(totalFiles).append("\n");
//                sb.append("Estimated total records (sum of DataFile.recordCount when available): ").append(totalRecords).append("\n");
//                sb.append("Estimated total bytes (sum of DataFile.fileSizeInBytes when available): ").append(totalBytes).append("\n");
//            } catch (Throwable t) {
//                sb.append("Failed to enumerate data files for summary: ").append(t.getMessage()).append("\n");
//            }
//
//            // Provide simple hint for queries
//            sb.append("\nHints:\n");
//            sb.append("  - This explain provides table-level metadata. For full SQL execution (SELECT rows), use Spark/Trino/Flink.\n");
//            sb.append("  - Lightweight COUNT(*) is supported by executeQuery by summing DataFile.recordCount; if recordCount is missing, result may be incomplete.\n");
//
//            return sb.toString();
//
//        } catch (Exception e) {
//            log.error("explainSql error: {}", e.getMessage(), e);
//            return "explainSql error: " + e.getMessage();
//        }
//    }
//
//    /**
//     * 尝试从 SQL 中提取第一个 FROM 后的表名（简单解析，支持反引号/点分隔）
//     */
//    private String extractTableName(String sql) {
//        Matcher m = FROM_PATTERN.matcher(sql);
//        if (m.find()) {
//            String raw = m.group(1);
//            if (raw == null) return null;
//            // 去掉可能的反引号
//            raw = raw.replaceAll("`", "");
//            return raw.trim();
//        }
//        return null;
//    }
//
//    /**
//     * 根据表名尝试加载 Iceberg Table。表名可能是：
//     * - catalog.ns.table （忽略 catalog，使用注入的 catalog）
//     * - db.table
//     * - table
//     *
//     * 兼容多形式 parse，尽量稳健处理。
//     */
//    private Table loadTableSafely(String tableName) {
//        try {
//            // 1) 尝试直接 parse（如果 TableIdentifier.parse 可用）
//            try {
//                // 尝试通过 TableIdentifier.parse 如果存在（部分Iceberg版本）
//                TableIdentifier id = null;
//                try {
//                    id = TableIdentifier.parse(tableName);
//                } catch (Throwable ignored) {
//                    // parse 方法不存在或失败，后面走手动构建
//                }
//                if (id != null) {
//                    return icebergCatalog.loadTable(id);
//                }
//            } catch (Throwable ignored) {
//            }
//
//            // 2) 手动拆分
//            String[] parts = tableName.split("\\.");
//            if (parts.length == 3) {
//                // 可能是 catalog.ns.table — 忽略 catalog 部分，使用注入的 catalog
//                String ns = parts[1];
//                String tbl = parts[2];
//                TableIdentifier id = TableIdentifier.of(Namespace.of(ns), tbl);
//                return icebergCatalog.loadTable(id);
//            } else if (parts.length == 2) {
//                String ns = parts[0];
//                String tbl = parts[1];
//                TableIdentifier id = TableIdentifier.of(Namespace.of(ns), tbl);
//                return icebergCatalog.loadTable(id);
//            } else {
//                // 单名表，尝试作为默认命名空间
//                String tbl = parts[0];
//                // 尝试 namespace = default, then namespace = current catalog default namespace
//                try {
//                    TableIdentifier id = TableIdentifier.of(Namespace.of(tbl), "");
//                    // 上面可能不是正确构造，回退
//                } catch (Throwable ignore) {}
//                // 一般情况下，single-part 在 Iceberg 里需要指定 namespace，尝试使用 Namespace.of("default")
//                try {
//                    TableIdentifier id = TableIdentifier.of(Namespace.of("default"), tbl);
//                    return icebergCatalog.loadTable(id);
//                } catch (Throwable ignored) {
//                }
//                // 最后尝试 TableIdentifier.of(Namespace.empty(), tbl)（部分版本支持）
//                try {
//                    TableIdentifier id = TableIdentifier.of(Namespace.empty(), tbl);
//                    return icebergCatalog.loadTable(id);
//                } catch (Throwable ignored) {
//                }
//            }
//        } catch (Exception e) {
//            log.warn("loadTableSafely failed for {}: {}", tableName, e.getMessage());
//        }
//        return null;
//    }
//}
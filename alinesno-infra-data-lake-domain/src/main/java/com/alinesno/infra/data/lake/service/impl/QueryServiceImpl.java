package com.alinesno.infra.data.lake.service.impl;

import com.alinesno.infra.data.lake.api.websql.QueryRequest;
import com.alinesno.infra.data.lake.api.websql.QueryResponse;
import com.alinesno.infra.data.lake.service.IQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.*;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.types.Types;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QueryServiceImpl - 支持:
 *  - COUNT(*) 基于 DataFile.recordCount 的快速统计
 *  - lightweight SELECT: 使用 IcebergGenerics 读取 records（支持 SELECT cols | SELECT *，简单 WHERE col = literal，LIMIT/OFFSET）
 *
 * 说明：不依赖 Spark，适合 small-preview 场景。
 */
@Slf4j
@Service
public class QueryServiceImpl implements IQueryService {

    @Resource
    @Qualifier("jdbcIcebergCatalog")
    private Catalog icebergCatalog;

    private static final Pattern FROM_PATTERN = Pattern.compile("(?i)\\bfrom\\s+([`\\w.\\-]+)");
    private static final Pattern SELECT_PATTERN = Pattern.compile("(?i)^\\s*select\\s+(.*?)\\s+from\\s", Pattern.DOTALL);
    private static final Pattern LIMIT_PATTERN = Pattern.compile("(?i)\\blimit\\s+(\\d+)");
    private static final Pattern OFFSET_PATTERN = Pattern.compile("(?i)\\boffset\\s+(\\d+)");
    // where col = 'a'  OR where col = "a" OR where col = 123
    private static final Pattern WHERE_EQUAL_PATTERN = Pattern.compile("(?i)\\bwhere\\s+([`\\w.\\-]+)\\s*=\\s*(?:'([^']*)'|\"([^\"]*)\"|([^\\s;]+))");

    @Override
    public QueryResponse executeQuery(QueryRequest request) {

//        org.apache.parquet.schema.MessageType m1 = null;
//        org.apache.iceberg.shaded.org.apache.parquet.schema.MessageType m2 = null;

        QueryResponse resp = new QueryResponse();
        try {
            String sql = request == null ? null : request.getSql();
            if (sql == null || sql.trim().isEmpty()) {
                resp.setError("sql is empty");
                resp.setTotal(-1L);
                return resp;
            }

            String lower = sql.trim().toLowerCase(Locale.ROOT);

            // COUNT(*) 支持（保留原实现）
            if (lower.matches("(?s).*\\bcount\\s*\\(.*\\).*")) {
                String tableName = extractTableName(sql);
                if (tableName == null) {
                    resp.setError("cannot parse table name from sql for count query");
                    resp.setTotal(-1L);
                    return resp;
                }

                Table table = loadTableSafely(tableName);
                if (table == null) {
                    resp.setError("table not found: " + tableName);
                    resp.setTotal(-1L);
                    return resp;
                }

                long totalRecords = 0L;
                long totalFiles = 0L;
                long totalBytes = 0L;
                try (CloseableIterable<FileScanTask> tasks = table.newScan().planFiles()) {
                    for (FileScanTask task : tasks) {
                        DataFile file = task.file();
                        if (file != null) {
                            totalFiles++;
                            try {
                                long rc = file.recordCount();
                                if (rc >= 0) totalRecords += rc;
                            } catch (Throwable t) {
                                log.debug("failed to read recordCount for file {}, reason: {}", file.path(), t.getMessage());
                            }
                            try {
                                long fs = file.fileSizeInBytes();
                                if (fs >= 0) totalBytes += fs;
                            } catch (Throwable t) {
                                log.debug("failed to read fileSize for file {}, reason: {}", file.path(), t.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("failed to plan files for table {}: {}", tableName, e.getMessage(), e);
                    resp.setError("failed to read table file list: " + e.getMessage());
                    resp.setTotal(-1L);
                    return resp;
                }

                Map<String, Object> row = new HashMap<>();
                row.put("count", totalRecords);
                resp.setColumns(Collections.singletonList("count"));
                resp.setRows(Collections.singletonList(row));
                resp.setTotal(totalRecords);
                resp.setError(null);
                return resp;
            }

            // SELECT 支持（lightweight via IcebergGenerics）
            if (lower.trim().startsWith("select")) {
                String tableName = extractTableName(sql);
                if (tableName == null) {
                    resp.setError("cannot parse table name from sql for select query");
                    resp.setTotal(-1L);
                    return resp;
                }

                Table table = loadTableSafely(tableName);
                if (table == null) {
                    resp.setError("table not found: " + tableName);
                    resp.setTotal(-1L);
                    return resp;
                }

                // 解析列
                List<String> selectCols = extractSelectColumns(sql);
                boolean selectAll = (selectCols == null || selectCols.isEmpty());

                if (selectAll) {
                    List<String> cols = new ArrayList<>();
                    for (Types.NestedField c : table.schema().columns()) {
                        cols.add(c.name());
                    }
                    selectCols = cols;
                }

                // 解析 limit/offset（并保护最大拉取数）
                Integer limit = extractLimit(sql);
                Integer offset = extractOffset(sql);
                final int DEFAULT_PAGE_SIZE = request != null && request.getPageSize() > 0 ? request.getPageSize() : 100;
                final int MAX_FETCH = 2000;
                if (limit == null) limit = Math.min(DEFAULT_PAGE_SIZE, MAX_FETCH);
                else limit = Math.min(limit, MAX_FETCH);
                if (offset == null) offset = 0;

                // 解析简单 where 等值
                Matcher whereMatcher = WHERE_EQUAL_PATTERN.matcher(sql);
                Expression icebergExpr = null;
                if (whereMatcher.find()) {
                    String col = whereMatcher.group(1).replace("`", "");
                    String v1 = whereMatcher.group(2);
                    String v2 = whereMatcher.group(3);
                    String v3 = whereMatcher.group(4);
                    String literalStr = (v1 != null) ? v1 : (v2 != null ? v2 : v3);
                    if (literalStr != null) {
                        Object lit = parseLiteral(literalStr);
                        if (lit != null) {
                            icebergExpr = Expressions.equal(col, lit);
                        }
                    }
                }

                // 构造 IcebergGenerics 读取器并执行（不显式使用 ReadBuilder 类型）
                CloseableIterable<Record> records;
                try {
                    // 将 selectCols 转为数组
                    String[] selectArr = (selectCols != null && !selectCols.isEmpty())
                            ? selectCols.toArray(new String[0])
                            : new String[0];

                    // 使用局部类型推断构建器（不会在代码中显式写 ReadBuilder）
                    var builder = IcebergGenerics.read(table);

                    if (selectArr.length > 0) {
                        builder = builder.select(selectArr);
                    }

                    if (icebergExpr != null) {
                        builder = builder.where(icebergExpr);
                    }

                    // 直接 build() 返回 CloseableIterable<Record>
                    records = builder.build();

                } catch (Throwable t) {
                    log.error("IcebergGenerics read build failed: {}", t.getMessage(), t);
                    resp.setError("Failed to prepare IcebergGenerics read: " + t.getMessage());
                    resp.setTotal(-1L);
                    return resp;
                }

//                // 迭代并执行 offset/limit
                List<Map<String, Object>> outRows = new ArrayList<>();
                int skipped = 0;
                int taken = 0;
//                try (CloseableIterable<Record> it = records) {
//                    for (Record rec : it) {
//                        if (skipped < offset) {
//                            skipped++;
//                            continue;
//                        }
//                        if (taken >= limit) break;
//
//                        Map<String, Object> row = new LinkedHashMap<>();
//                        for (String c : selectCols) {
//                            Object v = null;
//                            try {
//                                v = rec.get(c);
//                            } catch (Exception e) {
//                                // 如果通过名字获取失败，跳过该列值（复杂嵌套类型等场景）
//                                v = null;
//                            }
//                            row.put(c, v);
//                        }
//                        outRows.add(row);
//                        taken++;
//                    }
//                } catch (Throwable t) {
//                    log.error("Error iterating IcebergGenerics records: {}", t.getMessage(), t);
//                    resp.setError("Failed reading records: " + t.getMessage());
//                    resp.setTotal(-1L);
//                    return resp;
//                }

                // 在开始迭代前，构造列名->位置 的映射
                Map<String, Integer> nameToPos = new HashMap<>();
                String[] selectArr = !selectCols.isEmpty()
                        ? selectCols.toArray(new String[0])
                        : new String[0];

                if (selectArr.length > 0) {
                    // 如果使用了 select(...)，返回记录的顺序按 selectArr 顺序
                    for (int i = 0; i < selectArr.length; i++) {
                        nameToPos.put(selectArr[i], i);
                    }
                } else {
                    // select * 情况，按表 schema 顺序建立映射
                    List<Types.NestedField> cols = table.schema().columns();
                    for (int i = 0; i < cols.size(); i++) {
                        nameToPos.put(cols.get(i).name(), i);
                    }
                }

                // 迭代记录时按位置读取
                try (CloseableIterable<Record> it = records) {
                    for (Record rec : it) {
                        if (skipped < offset) {
                            skipped++;
                            continue;
                        }
                        if (taken >= limit) break;

                        Map<String, Object> row = new LinkedHashMap<>();
                        for (String c : selectCols) {
                            Object v = null;
                            Integer pos = nameToPos.get(c);
                            if (pos != null) {
                                try {
                                    v = rec.get(pos); // 使用位置索引读取
                                } catch (Throwable e) {
                                    // 读取失败则置 null（可能是复杂类型/嵌套）
                                    v = null;
                                }
                            } else {
                                // 严格来说不应该走到这里；作为兜底可尝试反射调用 get(String)（如果某些 Record 实现支持）
                                try {
                                    java.lang.reflect.Method m = rec.getClass().getMethod("get", String.class);
                                    v = m.invoke(rec, c);
                                } catch (Throwable ignore) {
                                    v = null;
                                }
                            }
                            row.put(c, v);
                        }
                        outRows.add(row);
                        taken++;
                    }
                }

                long estimatedTotal = estimateTotalRecords(table);

                resp.setColumns(selectCols);
                resp.setRows(outRows);
                resp.setTotal(estimatedTotal >= 0 ? estimatedTotal : outRows.size());
                resp.setError(null);
                return resp;
            }

            // 其它类型不支持
            resp.setError("Currently only COUNT(*) and lightweight SELECT (SELECT cols FROM table [WHERE col=val] [LIMIT/OFFSET]) are supported by this service. " +
                    "For full SQL support use Spark/Trino/Flink.");
            resp.setTotal(-1L);
            return resp;

        } catch (Exception ex) {
            log.error("executeQuery error: {}", ex.getMessage(), ex);
            resp.setError("executeQuery error: " + ex.getMessage());
            resp.setTotal(-1L);
            return resp;
        }
    }

    @Override
    public String explainSql(String sql, Object tableContext) {
        if (sql == null || sql.trim().isEmpty()) {
            return "sql is empty";
        }
        try {
            String tableName = extractTableName(sql);
            if (tableName == null) {
                return "Cannot parse table name from SQL. Please provide a SQL like: SELECT ... FROM db.table";
            }

            Table table = loadTableSafely(tableName);
            if (table == null) {
                return "Table not found: " + tableName;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Table: ").append(tableName).append("\n");

            String idStr = null;
            try {
                Method m = Table.class.getMethod("identifier");
                Object idObj = m.invoke(table);
                idStr = idObj == null ? null : String.valueOf(idObj);
            } catch (NoSuchMethodException nsme) {
                try {
                    Method m2 = Table.class.getMethod("name");
                    Object idObj = m2.invoke(table);
                    idStr = idObj == null ? null : String.valueOf(idObj);
                } catch (NoSuchMethodException nsme2) {
                    idStr = table.toString();
                } catch (Throwable t2) {
                    idStr = table.toString();
                }
            } catch (Throwable t) {
                idStr = table.toString();
            }
            sb.append("Identifier/Name: ").append(idStr).append("\n");

            // Schema
            Schema schema = table.schema();
            sb.append("Schema:\n");
            schema.columns().forEach(c -> sb.append("  - ").append(c.name()).append(" : ").append(c.type()).append("\n"));

            // Partition spec
            try {
                sb.append("Partition spec: ").append(table.spec().toString()).append("\n");
            } catch (Throwable t) {
                sb.append("Partition spec: <unavailable: ").append(t.getMessage()).append(">\n");
            }

            // Snapshot info
            try {
                Snapshot snap = table.currentSnapshot();
                if (snap != null) {
                    sb.append("Current snapshot id: ").append(snap.snapshotId()).append("\n");
                    sb.append("  sequence number: ").append(snap.sequenceNumber()).append("\n");
                    sb.append("  manifest list: ").append(snap.manifestListLocation()).append("\n");
                    sb.append("  summary:\n");
                    Map<String, String> summary = snap.summary();
                    if (summary != null) {
                        summary.forEach((k, v) -> sb.append("    ").append(k).append(" = ").append(v).append("\n"));
                    }
                } else {
                    sb.append("No current snapshot\n");
                }
            } catch (Throwable t) {
                sb.append("Snapshot info: <unavailable: ").append(t.getMessage()).append(">\n");
            }

            long totalFiles = 0L;
            long totalRecords = 0L;
            long totalBytes = 0L;
            try (CloseableIterable<FileScanTask> tasks = table.newScan().planFiles()) {
                for (FileScanTask task : tasks) {
                    DataFile f = task.file();
                    if (f != null) {
                        totalFiles++;
                        try {
                            long rc = f.recordCount();
                            if (rc > 0) totalRecords += rc;
                        } catch (Throwable ignore) {}
                        try {
                            long fs = f.fileSizeInBytes();
                            if (fs > 0) totalBytes += fs;
                        } catch (Throwable ignore) {}
                    }
                }
                sb.append("Data files (planned): ").append(totalFiles).append("\n");
                sb.append("Estimated total records (sum of DataFile.recordCount when available): ").append(totalRecords).append("\n");
                sb.append("Estimated total bytes (sum of DataFile.fileSizeInBytes when available): ").append(totalBytes).append("\n");
            } catch (Throwable t) {
                sb.append("Failed to enumerate data files for summary: ").append(t.getMessage()).append("\n");
            }

            sb.append("\nHints:\n");
            sb.append("  - This explain provides table-level metadata. For full SQL execution (SELECT rows), use Spark/Trino/Flink.\n");
            sb.append("  - Lightweight COUNT(*) is supported by executeQuery by summing DataFile.recordCount; if recordCount is missing, result may be incomplete.\n");

            return sb.toString();

        } catch (Exception e) {
            log.error("explainSql error: {}", e.getMessage(), e);
            return "explainSql error: " + e.getMessage();
        }
    }

    // 辅助方法 --------------------------------------------------------

    private String extractTableName(String sql) {
        Matcher m = FROM_PATTERN.matcher(sql);
        if (m.find()) {
            String raw = m.group(1);
            if (raw == null) return null;
            raw = raw.replaceAll("`", "");
            return raw.trim();
        }
        return null;
    }

    private List<String> extractSelectColumns(String sql) {
        Matcher m = SELECT_PATTERN.matcher(sql);
        if (m.find()) {
            String cols = m.group(1);
            if (cols == null) return Collections.emptyList();
            cols = cols.trim();
            if ("*".equals(cols)) return Collections.emptyList(); // select *
            String[] parts = cols.split(",");
            List<String> out = new ArrayList<>();
            for (String p : parts) {
                String c = p.trim();
                // 移除 alias（简单规则）
                c = c.replaceAll("(?i)\\s+as\\s+.*$", "");
                // 移除可能的 alias（最简单处理）
                String[] sp = c.split("\\s+");
                c = sp[0];
                c = c.replaceAll("`", "");
                out.add(c.trim());
            }
            return out;
        }
        return Collections.emptyList();
    }

    private Integer extractLimit(String sql) {
        Matcher m = LIMIT_PATTERN.matcher(sql);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Integer extractOffset(String sql) {
        Matcher m = OFFSET_PATTERN.matcher(sql);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Object parseLiteral(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        // 尝试数字
        try {
            if (v.matches("^-?\\d+$")) {
                return Long.parseLong(v);
            }
            if (v.matches("^-?\\d+\\.\\d+$")) {
                return Double.parseDouble(v);
            }
        } catch (Exception ignored) {}
        return v;
    }

    private long estimateTotalRecords(Table table) {
        long totalRecords = 0L;
        try (CloseableIterable<FileScanTask> tasks = table.newScan().planFiles()) {
            for (FileScanTask task : tasks) {
                DataFile f = task.file();
                if (f != null) {
                    try {
                        long rc = f.recordCount();
                        if (rc >= 0) totalRecords += rc;
                    } catch (Throwable ignore) {}
                }
            }
            return totalRecords;
        } catch (Throwable t) {
            log.debug("estimateTotalRecords failed: {}", t.getMessage());
            return -1L;
        }
    }

    private Table loadTableSafely(String tableName) {
        try {
            try {
                TableIdentifier id = null;
                try {
                    id = TableIdentifier.parse(tableName);
                } catch (Throwable ignored) {}
                if (id != null) {
                    return icebergCatalog.loadTable(id);
                }
            } catch (Throwable ignored) {}

            String[] parts = tableName.split("\\.");
            if (parts.length == 3) {
                String ns = parts[1];
                String tbl = parts[2];
                TableIdentifier id = TableIdentifier.of(Namespace.of(ns), tbl);
                return icebergCatalog.loadTable(id);
            } else if (parts.length == 2) {
                String ns = parts[0];
                String tbl = parts[1];
                TableIdentifier id = TableIdentifier.of(Namespace.of(ns), tbl);
                return icebergCatalog.loadTable(id);
            } else {
                String tbl = parts[0];
                try {
                    TableIdentifier id = TableIdentifier.of(Namespace.of("default"), tbl);
                    return icebergCatalog.loadTable(id);
                } catch (Throwable ignored) {}
                try {
                    TableIdentifier id = TableIdentifier.of(Namespace.empty(), tbl);
                    return icebergCatalog.loadTable(id);
                } catch (Throwable ignored) {}
            }
        } catch (Exception e) {
            log.warn("loadTableSafely failed for {}: {}", tableName, e.getMessage());
        }
        return null;
    }
}
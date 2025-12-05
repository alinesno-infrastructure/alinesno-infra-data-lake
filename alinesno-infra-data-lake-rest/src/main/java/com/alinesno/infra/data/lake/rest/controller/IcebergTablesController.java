package com.alinesno.infra.data.lake.rest.controller;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.NoSuchTableException;
import org.apache.iceberg.types.Types;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Iceberg 表控制器：列表、创建（简单 schema 支持）、获取元数据、删除、重命名、列出快照
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class IcebergTablesController {

    @Resource(name="jdbcIcebergCatalog")
    private final Catalog catalog;


    // Helper: parse namespace like "a.b" or "a/b"
    private Namespace parseNamespace(String ns) {
        if (ns == null || ns.trim().isEmpty()) {
            return Namespace.empty();
        }
        String normalized = ns.replace('/', '.');
        String[] parts = Arrays.stream(normalized.split("\\."))
                .filter(s -> s != null && !s.isEmpty())
                .toArray(String[]::new);
        return Namespace.of(parts);
    }

    // 列出某个命名空间下的所有表
    @GetMapping
    public ResponseEntity<?> listTables(@RequestParam(name = "namespace") String namespace) {
        try {
            Namespace ns = parseNamespace(namespace);
            List<String> tables = catalog.listTables(ns)
                    .stream()
                    .map(TableIdentifier::toString)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Collections.singletonMap("tables", tables));
        } catch (Exception e) {
            log.error("listTables failed for {}", namespace, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // 获取表元数据（schema, partition, location, properties, snapshot count）
    @GetMapping("/{namespace}/{table}")
    public ResponseEntity<?> getTableMeta(@PathVariable("namespace") String namespace,
                                          @PathVariable("table") String table) {
        Namespace ns = parseNamespace(namespace);
        TableIdentifier id = TableIdentifier.of(ns, table);
        try {
            Table t = catalog.loadTable(id);
            Map<String, Object> meta = new HashMap<>();
            meta.put("name", t.name());
            meta.put("schema", t.schema().toString());
            meta.put("partitionSpec", t.spec().toString());
            meta.put("location", t.location());
            meta.put("properties", t.properties());
            // snapshots count
            int snapshots = 0;
            for (Snapshot s : t.snapshots()) snapshots++;
            meta.put("snapshots", snapshots);
            return ResponseEntity.ok(meta);
        } catch (NoSuchTableException nsEx) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "table not found: " + id.toString()));
        } catch (Exception e) {
            log.error("getTableMeta failed for {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // 创建表：如果没有 body，使用默认 schema (id: long)；可通过 body 提供简单字段列表
    // 示例 body:
    // {
    //   "columns": [
    //     {"name":"id", "type":"long", "required": true},
    //     {"name":"name", "type":"string", "required": false}
    //   ],
    //   "partition": "none"  // 暂仅支持 "none"
    // }
    public static class CreateTableRequest {
        public List<Column> columns;
        public String partition; // 暂保留
    }

    public static class Column {
        public String name;
        public String type;
        public Boolean required;
    }

    private Types.NestedField buildField(int id, Column c) {
        String t = c.type == null ? "string" : c.type.toLowerCase(Locale.ROOT);
        boolean required = c.required != null && c.required;
        return switch (t) {
            case "long", "bigint" -> required ? Types.NestedField.required(id, c.name, Types.LongType.get())
                    : Types.NestedField.optional(id, c.name, Types.LongType.get());
            case "int", "integer" -> required ? Types.NestedField.required(id, c.name, Types.IntegerType.get())
                    : Types.NestedField.optional(id, c.name, Types.IntegerType.get());
            case "double" -> required ? Types.NestedField.required(id, c.name, Types.DoubleType.get())
                    : Types.NestedField.optional(id, c.name, Types.DoubleType.get());
            case "float" -> required ? Types.NestedField.required(id, c.name, Types.FloatType.get())
                    : Types.NestedField.optional(id, c.name, Types.FloatType.get());
            case "boolean", "bool" -> required ? Types.NestedField.required(id, c.name, Types.BooleanType.get())
                    : Types.NestedField.optional(id, c.name, Types.BooleanType.get());
            default -> required ? Types.NestedField.required(id, c.name, Types.StringType.get())
                    : Types.NestedField.optional(id, c.name, Types.StringType.get());
        };
    }

    @PostMapping("/{namespace}/{table}")
    public ResponseEntity<?> createTable(@PathVariable("namespace") String namespace,
                                         @PathVariable("table") String table,
                                         @RequestBody(required = false) CreateTableRequest req) {
        Namespace ns = parseNamespace(namespace);
        TableIdentifier id = TableIdentifier.of(ns, table);
        try {
            if (catalog.tableExists(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("error", "table already exists: " + id.toString()));
            }

            Schema schema;
            if (req == null || req.columns == null || req.columns.isEmpty()) {
                schema = new Schema(Types.NestedField.required(1, "id", Types.LongType.get()));
            } else {
                List<Types.NestedField> fields = new ArrayList<>();
                int fid = 1;
                for (Column c : req.columns) {
                    fields.add(buildField(fid++, c));
                }
                schema = new Schema(fields);
            }

            PartitionSpec spec = PartitionSpec.unpartitioned(); // 简单处理
            Map<String, String> props = new HashMap<>();
            props.put("format-version", "2");

            Table t = catalog.createTable(id, schema, spec, props);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Collections.singletonMap("table", t.name()));
        } catch (Exception e) {
            log.error("createTable failed for {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // 删除表，可选择 purge (是否删除数据) - 通过 request param 控制
    @DeleteMapping("/{namespace}/{table}")
    public ResponseEntity<?> dropTable(@PathVariable("namespace") String namespace,
                                       @PathVariable("table") String table,
                                       @RequestParam(name = "purge", defaultValue = "false") boolean purge) {
        Namespace ns = parseNamespace(namespace);
        TableIdentifier id = TableIdentifier.of(ns, table);
        try {
            boolean dropped = catalog.dropTable(id, purge);
            return ResponseEntity.ok(Collections.singletonMap("dropped", dropped));
        } catch (Exception e) {
            log.error("dropTable failed for {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // 重命名表：body {"newNamespace":"a.b","newTable":"newName"}，若 newNamespace 未传，使用原 namespace
    public static class RenameRequest {
        public String newNamespace;
        public String newTable;
    }

    @PostMapping("/{namespace}/{table}/rename")
    public ResponseEntity<?> renameTable(@PathVariable("namespace") String namespace,
                                         @PathVariable("table") String table,
                                         @RequestBody RenameRequest req) {
        Namespace srcNs = parseNamespace(namespace);
        TableIdentifier srcId = TableIdentifier.of(srcNs, table);
        String targetTable = req.newTable == null ? table : req.newTable;
        Namespace targetNs = req.newNamespace == null ? srcNs : parseNamespace(req.newNamespace);
        TableIdentifier targetId = TableIdentifier.of(targetNs, targetTable);
        try {
            catalog.renameTable(srcId, targetId);
            return ResponseEntity.ok(Collections.singletonMap("renamedTo", targetId.toString()));
        } catch (Exception e) {
            log.error("renameTable failed from {} to {}", srcId, targetId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // 列出表的快照信息（id, timestamp, summary）
    @GetMapping("/{namespace}/{table}/snapshots")
    public ResponseEntity<?> listSnapshots(@PathVariable("namespace") String namespace,
                                           @PathVariable("table") String table) {
        Namespace ns = parseNamespace(namespace);
        TableIdentifier id = TableIdentifier.of(ns, table);
        try {
            Table t = catalog.loadTable(id);
            List<Map<String, Object>> snaps = new ArrayList<>();
            for (Snapshot s : t.snapshots()) {
                Map<String, Object> m = new HashMap<>();
                m.put("snapshotId", s.snapshotId());
                m.put("timestampMillis", s.timestampMillis());
                m.put("summary", s.summary());
                m.put("manifestListLocation", s.manifestListLocation());
                snaps.add(m);
            }
            return ResponseEntity.ok(Collections.singletonMap("snapshots", snaps));
        } catch (NoSuchTableException nsEx) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "table not found: " + id.toString()));
        } catch (Exception e) {
            log.error("listSnapshots failed for {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
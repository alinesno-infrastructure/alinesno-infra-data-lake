package com.alinesno.infra.data.lake.rest.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.SupportsNamespaces;
import org.apache.iceberg.catalog.TableIdentifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/namespaces")
public class IcebergNamespacesController {

    // 用于命名空间操作（创建、删除、列出命名空间）
    private final SupportsNamespaces namespaceCatalog;

    // 用于表操作（列出命名空间下的表）
    private final Catalog tableCatalog;

    // 注入同一个 Catalog 实例，同时适配两个接口（需确保 Catalog 实现了 SupportsNamespaces）
    public IcebergNamespacesController(
            @Qualifier("jdbcIcebergCatalog") SupportsNamespaces namespaceCatalog,
            @Qualifier("jdbcIcebergCatalog") Catalog tableCatalog) {
        this.namespaceCatalog = namespaceCatalog;
        this.tableCatalog = tableCatalog;
    }

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

    @GetMapping
    public ResponseEntity<List<String>> listNamespaces() {
        try {
            // 用 SupportsNamespaces 列出根命名空间下的所有命名空间
            List<String> names = namespaceCatalog.listNamespaces(Namespace.empty())
                    .stream()
                    .map(n -> String.join(".", n.levels()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(names);
        } catch (Exception e) {
            log.error("listNamespaces failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/{namespace}")
    public ResponseEntity<?> getNamespaceInfo(@PathVariable("namespace") String namespace) {
        Namespace ns = parseNamespace(namespace);
        try {
            String nsString = String.join(".", ns.levels());
            // 检查命名空间是否存在（用 SupportsNamespaces 接口）
            boolean exists = namespaceCatalog.listNamespaces(Namespace.empty())
                    .stream()
                    .map(n -> String.join(".", n.levels()))
                    .anyMatch(s -> s.equals(nsString));

            if (!exists) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "namespace not found: " + nsString));
            }

            // 列出命名空间下的表（用 Catalog 接口的 listTables 方法）
            List<String> tables = tableCatalog.listTables(ns)
                    .stream()
                    .map(TableIdentifier::toString)
                    .collect(Collectors.toList());

            Map<String, Object> resp = new HashMap<>();
            resp.put("namespace", nsString);
            resp.put("exists", true);
            resp.put("tablesCount", tables.size());
            resp.put("tables", tables);

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("getNamespaceInfo failed for {}", namespace, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/{namespace}")
    public ResponseEntity<?> createNamespace(@PathVariable("namespace") String namespace,
                                             @RequestBody(required = false) Map<String, String> properties) {
        Namespace ns = parseNamespace(namespace);
        try {
            Map<String, String> props = properties == null ? Collections.emptyMap() : properties;
            // 创建命名空间（用 SupportsNamespaces 接口）
            namespaceCatalog.createNamespace(ns, props);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Collections.singletonMap("namespace", String.join(".", ns.levels())));
        } catch (Exception e) {
            log.error("createNamespace failed for {}", namespace, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{namespace}")
    public ResponseEntity<?> dropNamespace(@PathVariable("namespace") String namespace) {
        Namespace ns = parseNamespace(namespace);
        try {
            // 删除命名空间（用 SupportsNamespaces 接口）
            boolean dropped = namespaceCatalog.dropNamespace(ns);
            return ResponseEntity.ok(Collections.singletonMap("dropped", dropped));
        } catch (Exception e) {
            log.error("dropNamespace failed for {}", namespace, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
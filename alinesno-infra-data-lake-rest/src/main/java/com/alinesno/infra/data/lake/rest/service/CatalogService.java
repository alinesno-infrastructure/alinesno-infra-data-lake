//package com.alinesno.infra.data.lake.rest.service;
//
//import com.alinesno.infra.data.lake.api.CatalogDto;
//import com.alinesno.infra.data.lake.rest.dto.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.iceberg.PartitionField;
//import org.apache.iceberg.Schema;
//import org.apache.iceberg.Table;
//import org.apache.iceberg.catalog.Catalog;
//import org.apache.iceberg.catalog.Namespace;
//import org.apache.iceberg.catalog.TableIdentifier;
//import org.apache.iceberg.PartitionSpec;
//import org.apache.iceberg.types.Types;
//import org.springframework.stereotype.Service;
//
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.time.Instant;
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * CatalogService for Iceberg-based metadata operations.
// *
// * Note:
// * - Some Iceberg Catalog APIs (createNamespace / dropNamespace) are not present in all versions.
// * - This implementation uses reflection to call optional namespace methods when available.
// * - If createNamespace/dropNamespace don't exist, we fallback to no-op for creation and to
// *   deleting all tables under the namespace for drop.
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CatalogService {
//
//    private final Catalog icebergCatalog;
//
//    public void createCatalog(CreateCatalogRequest req) {
//        Namespace ns = Namespace.of(req.getName());
//        Map<String, String> props = new HashMap<>();
//        if (req.getComment() != null) props.put("comment", req.getComment());
//        props.put("createTime", String.valueOf(Instant.now().toEpochMilli()));
//
//        // Try to call createNamespace(Namespace, Map) or createNamespace(Namespace) via reflection.
//        try {
//            Method mWithProps = findMethod(icebergCatalog.getClass(), "createNamespace", Namespace.class, Map.class);
//            if (mWithProps != null) {
//                mWithProps.invoke(icebergCatalog, ns, props);
//                return;
//            }
//            Method mNoProps = findMethod(icebergCatalog.getClass(), "createNamespace", Namespace.class);
//            if (mNoProps != null) {
//                mNoProps.invoke(icebergCatalog, ns);
//                return;
//            }
//            // If createNamespace isn't available, we treat catalog creation as a no-op.
//            log.debug("createNamespace not available on Catalog implementation, skipping explicit namespace creation for {}", req.getName());
//        } catch (InvocationTargetException ite) {
//            throw new IllegalArgumentException("create catalog failed: " + ite.getCause().getMessage(), ite.getCause());
//        } catch (Exception e) {
//            throw new RuntimeException("create catalog failed", e);
//        }
//    }
//
//    public List<CatalogDto> listCatalogs() {
//        Set<String> names = new LinkedHashSet<>();
//
//        // Try listNamespaces() via reflection
//        try {
//            Object nsObj = safeInvoke(icebergCatalog, "listNamespaces");
//            if (nsObj != null) {
//                if (nsObj instanceof Namespace[]) {
//                    for (Namespace ns : (Namespace[]) nsObj) {
//                        String[] levels = ns.levels();
//                        if (levels.length >= 1) names.add(levels[0]);
//                    }
//                } else if (nsObj instanceof Iterable) {
//                    for (Object o : (Iterable<?>) nsObj) {
//                        if (o instanceof Namespace) {
//                            String[] levels = ((Namespace) o).levels();
//                            if (levels.length >= 1) names.add(levels[0]);
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.warn("listNamespaces via reflection failed, fallback will try scanning listTables", e);
//        }
//
//        // If reflection didn't discover namespaces, try fallback by listing tables and extracting first-level namespace.
//        if (names.isEmpty()) {
//            try {
//                // Try to list all namespaces by calling listNamespaces() that returns Namespace[] (again fallback)
//                Namespace[] arr = tryListNamespacesArray();
//                if (arr != null) {
//                    for (Namespace ns : arr) {
//                        String[] levels = ns.levels();
//                        if (levels.length >= 1) names.add(levels[0]);
//                    }
//                } else {
//                    // As last resort, try to list tables for default/no-arg; if not supported, nothing we can do generically.
//                    log.debug("Unable to enumerate namespaces via Catalog API on this implementation");
//                }
//            } catch (Exception ignore) {
//            }
//        }
//
//        List<CatalogDto> res = new ArrayList<>();
//        for (String n : names) {
//            CatalogDto dto = CatalogDto.builder().name(n).type("iceberg").build();
//            res.add(dto);
//        }
//        return res;
//    }
//
//    public CatalogDto getCatalog(String name) {
//        // Iceberg does not necessarily keep a rich "catalog" object; return minimal info.
//        return CatalogDto.builder().name(name).type("iceberg").build();
//    }
//
//    public void deleteCatalog(String name) {
//        // Attempt to delete all namespaces and tables under the given first-level name.
//        try {
//            Namespace[] all = tryListNamespacesArray();
//            if (all == null) {
//                log.debug("No namespaces discovered; nothing to delete for catalog {}", name);
//                return;
//            }
//            for (Namespace ns : all) {
//                String[] levels = ns.levels();
//                if (levels.length >= 1 && name.equals(levels[0])) {
//                    // Drop all tables under this namespace
//                    List<TableIdentifier> tables = icebergCatalog.listTables(ns);
//                    for (TableIdentifier tid : tables) {
//                        try {
//                            icebergCatalog.dropTable(tid);
//                        } catch (Exception ignore) {
//                        }
//                    }
//                    // Try to drop namespace via reflection if available, otherwise skip (tables already dropped)
//                    try {
//                        Method dropNs = findMethod(icebergCatalog.getClass(), "dropNamespace", Namespace.class);
//                        if (dropNs != null) {
//                            dropNs.invoke(icebergCatalog, ns);
//                        } else {
//                            log.debug("dropNamespace not available on Catalog implementation, skipped for {}", ns);
//                        }
//                    } catch (InvocationTargetException ite) {
//                        log.warn("dropNamespace invocation caused error (ignored): {}", ite.getCause().getMessage());
//                    } catch (Exception ignore) {
//                    }
//                }
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("delete catalog failed", e);
//        }
//    }
//
//    public void createDatabase(String catalogName, CreateDatabaseRequest req) {
//        Namespace ns = Namespace.of(catalogName, req.getName());
//        Map<String, String> props = new HashMap<>();
//        if (req.getComment() != null) props.put("comment", req.getComment());
//        props.put("createTime", String.valueOf(Instant.now().toEpochMilli()));
//
//        try {
//            Method mWithProps = findMethod(icebergCatalog.getClass(), "createNamespace", Namespace.class, Map.class);
//            if (mWithProps != null) {
//                mWithProps.invoke(icebergCatalog, ns, props);
//                return;
//            }
//            Method mNoProps = findMethod(icebergCatalog.getClass(), "createNamespace", Namespace.class);
//            if (mNoProps != null) {
//                mNoProps.invoke(icebergCatalog, ns);
//                return;
//            }
//            // If not supported, treat as no-op: many Catalog implementations do not require explicit namespace creation.
//            log.debug("createNamespace not available, skipping explicit namespace creation for {}", ns);
//        } catch (InvocationTargetException ite) {
//            throw new IllegalArgumentException("create database failed: " + ite.getCause().getMessage(), ite.getCause());
//        } catch (Exception e) {
//            throw new RuntimeException("create database failed", e);
//        }
//    }
//
//    public List<DatabaseDto> listDatabases(String catalogName) {
//        List<DatabaseDto> res = new ArrayList<>();
//        try {
//            // Try listNamespaces(Namespace.of(catalogName)) via reflection
//            Method listNsWithArg = findMethod(icebergCatalog.getClass(), "listNamespaces", Namespace.class);
//            Namespace[] arr = null;
//            if (listNsWithArg != null) {
//                Object out = listNsWithArg.invoke(icebergCatalog, Namespace.of(catalogName));
//                if (out instanceof Namespace[]) {
//                    arr = (Namespace[]) out;
//                } else if (out instanceof Iterable) {
//                    List<Namespace> tmp = new ArrayList<>();
//                    for (Object o : (Iterable<?>) out) {
//                        if (o instanceof Namespace) tmp.add((Namespace) o);
//                    }
//                    arr = tmp.toArray(new Namespace[0]);
//                }
//            } else {
//                // fallback to all namespaces and filter by prefix
//                arr = tryListNamespacesArray();
//            }
//
//            if (arr != null) {
//                Set<String> dbs = new LinkedHashSet<>();
//                for (Namespace ns : arr) {
//                    String[] levels = ns.levels();
//                    if (levels.length >= 2 && catalogName.equals(levels[0])) {
//                        dbs.add(levels[1]);
//                    }
//                }
//                for (String db : dbs) {
//                    DatabaseDto d = DatabaseDto.builder()
//                            .catalogName(catalogName)
//                            .name(db)
//                            .build();
//                    res.add(d);
//                }
//            }
//        } catch (InvocationTargetException ite) {
//            throw new RuntimeException("list databases failed", ite.getCause());
//        } catch (Exception e) {
//            throw new RuntimeException("list databases failed", e);
//        }
//        return res;
//    }
//
//    public void dropDatabase(String catalogName, String dbName, boolean cascade) {
//        Namespace ns = Namespace.of(catalogName, dbName);
//        try {
//            List<TableIdentifier> tables = icebergCatalog.listTables(ns);
//            if (!cascade && !tables.isEmpty()) {
//                throw new IllegalStateException("database not empty");
//            }
//            for (TableIdentifier t : tables) {
//                try {
//                    icebergCatalog.dropTable(t);
//                } catch (Exception ignore) {
//                }
//            }
//            // Try to drop namespace via reflection if available
//            Method dropNs = findMethod(icebergCatalog.getClass(), "dropNamespace", Namespace.class);
//            if (dropNs != null) {
//                dropNs.invoke(icebergCatalog, ns);
//            } else {
//                log.debug("dropNamespace not available on Catalog implementation, namespace drop skipped for {}", ns);
//            }
//        } catch (InvocationTargetException ite) {
//            throw new RuntimeException("drop database failed", ite.getCause());
//        } catch (Exception e) {
//            throw new RuntimeException("drop database failed", e);
//        }
//    }
//
//    public void createTable(String catalogName, String dbName, CreateTableRequest req) {
//        Namespace ns = Namespace.of(catalogName, dbName);
//        TableIdentifier id = TableIdentifier.of(ns, req.getTableName());
//
//        Schema schema = null ; // buildIcebergSchema(req.getSchema());
//        PartitionSpec spec = PartitionSpec.unpartitioned();
//        Map<String, String> props = req.getProperties() == null ? Collections.emptyMap() : req.getProperties();
//
//        try {
//            // Try common signatures via reflection
//            Method mMap = findMethod(icebergCatalog.getClass(), "createTable",
//                    TableIdentifier.class, Schema.class, PartitionSpec.class, Map.class);
//            if (mMap != null) {
//                mMap.invoke(icebergCatalog, id, schema, spec, props);
//                return;
//            }
//            Method mLoc = findMethod(icebergCatalog.getClass(), "createTable",
//                    TableIdentifier.class, Schema.class, PartitionSpec.class, String.class, Map.class);
//            if (mLoc != null) {
//                mLoc.invoke(icebergCatalog, id, schema, spec, (String) null, props);
//                return;
//            }
//            // Fallback to direct call if available at compile time
//            icebergCatalog.createTable(id, schema, spec, props);
//        } catch (InvocationTargetException ite) {
//            throw new IllegalArgumentException("create table failed: " + ite.getCause().getMessage(), ite.getCause());
//        } catch (NoSuchMethodError nsme) {
//            throw new IllegalArgumentException("create table API not supported by Catalog implementation", nsme);
//        } catch (Exception e) {
//            throw new RuntimeException("create table failed", e);
//        }
//    }
//
//    public List<TableDto> listTables(String catalogName, String dbName) {
//        Namespace ns = Namespace.of(catalogName, dbName);
//        List<TableDto> res = new ArrayList<>();
//        try {
//            List<TableIdentifier> ids = icebergCatalog.listTables(ns);
//            for (TableIdentifier id : ids) {
//                try {
//                    Table t = icebergCatalog.loadTable(id);
//                    TableDto dto = TableDto.builder()
//                            .catalogName(catalogName)
//                            .databaseName(dbName)
//                            .tableName(id.name())
//                            .schema(convertIcebergSchemaToDto(t.schema(), t.spec()))
//                            .properties(t.properties())
//                            .build();
//                    res.add(dto);
//                } catch (Exception ignore) {
//                }
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("list tables failed", e);
//        }
//        return res;
//    }
//
//    public TableDto getTable(String catalogName, String dbName, String tableName) {
//        TableIdentifier id = TableIdentifier.of(Namespace.of(catalogName, dbName), tableName);
//        try {
//            Table t = icebergCatalog.loadTable(id);
//            return TableDto.builder()
//                    .catalogName(catalogName)
//                    .databaseName(dbName)
//                    .tableName(tableName)
//                    .schema(convertIcebergSchemaToDto(t.schema(), t.spec()))
//                    .properties(t.properties())
//                    .build();
//        } catch (org.apache.iceberg.exceptions.NoSuchTableException ex) {
//            return null;
//        } catch (Exception e) {
//            throw new RuntimeException("get table failed", e);
//        }
//    }
//
//    public void dropTable(String catalogName, String dbName, String tableName) {
//        TableIdentifier id = TableIdentifier.of(Namespace.of(catalogName, dbName), tableName);
//        try {
//            icebergCatalog.dropTable(id);
//        } catch (Exception e) {
//            throw new RuntimeException("drop table failed", e);
//        }
//    }
//
//    // ---------------- helper ----------------
//
//    private Schema buildIcebergSchema(List<FieldDto> fields) {
//        if (fields == null || fields.isEmpty()) {
//            return new Schema(Collections.emptyList());
//        }
//        List<Types.NestedField> cols = new ArrayList<>();
//        int id = 1;
//        for (FieldDto f : fields) {
//            Types.NestedField nf;
//            if (f.isNullable()) {
//                nf = Types.NestedField.optional(id, f.getName(), parseType(f.getType()), f.getComment());
//            } else {
//                nf = Types.NestedField.required(id, f.getName(), parseType(f.getType()), f.getComment());
//            }
//            cols.add(nf);
//            id++;
//        }
//        return new Schema(cols);
//    }
//
//    private org.apache.iceberg.types.Type parseType(String t) {
//        if (t == null) return Types.StringType.get();
//        String lower = t.toLowerCase().trim();
//        if (lower.startsWith("varchar") || lower.startsWith("string") || lower.startsWith("char")) {
//            return Types.StringType.get();
//        } else if (lower.equals("int") || lower.equals("integer")) {
//            return Types.IntegerType.get();
//        } else if (lower.equals("bigint") || lower.equals("long")) {
//            return Types.LongType.get();
//        } else if (lower.equals("float")) {
//            return Types.FloatType.get();
//        } else if (lower.equals("double")) {
//            return Types.DoubleType.get();
//        } else if (lower.equals("boolean") || lower.equals("bool")) {
//            return Types.BooleanType.get();
//        } else if (lower.equals("date")) {
//            return Types.DateType.get();
//        } else if (lower.equals("time")) {
//            return Types.TimeType.get();
//        } else if (lower.equals("timestamp")) {
//            try {
//                return Types.TimestampType.withZone();
//            } catch (NoSuchMethodError e) {
//                return Types.TimestampType.withoutZone();
//            }
//        } else if (lower.startsWith("decimal")) {
//            Pattern p = Pattern.compile("decimal\\((\\d+)\\s*,\\s*(\\d+)\\)");
//            Matcher m = p.matcher(lower);
//            if (m.find()) {
//                int precision = Integer.parseInt(m.group(1));
//                int scale = Integer.parseInt(m.group(2));
//                return Types.DecimalType.of(precision, scale);
//            }
//            return Types.DecimalType.of(10, 2);
//        }
//        return Types.StringType.get();
//    }
//
//    private TableSchemaDto convertIcebergSchemaToDto(Schema schema, PartitionSpec spec) {
//        List<FieldDto> fields = new ArrayList<>();
//        if (schema != null) {
//            for (Types.NestedField f : schema.columns()) {
//                FieldDto dto = new FieldDto();
//                dto.setName(f.name());
//                dto.setNullable(!f.isRequired());
//                dto.setComment(f.doc());
//                dto.setType(f.type().toString());
//                fields.add(dto);
//            }
//        }
//
//        List<String> partitionKeys = new ArrayList<>();
//        if (spec != null) {
//            for (PartitionField pf : spec.fields()) {
//                // PartitionField.name() 在常见版本中存在；若不存在请改为 pf.fieldName() 或者相应 API
//                try {
//                    partitionKeys.add(pf.name());
//                } catch (NoSuchMethodError | AbstractMethodError ex) {
//                    // 兼容性保护：尝试其他可能的 method 名称
//                    try {
//                        // fallback: some versions have sourceName/fieldName etc.
//                        Method m = pf.getClass().getMethod("fieldName");
//                        Object res = m.invoke(pf);
//                        if (res != null) partitionKeys.add(res.toString());
//                    } catch (Exception ignore) {
//                    }
//                }
//            }
//        }
//
//        return TableSchemaDto.builder()
//                .fields(fields)
//                .partitionKeys(partitionKeys)
//                .primaryKey(null) // Iceberg primary key support varies; 若你需要从元数据读取，请在这里实现
//                .build();
//    }
//
//    // reflection helpers
//
//    private Method findMethod(Class<?> cls, String name, Class<?>... paramTypes) {
//        try {
//            return cls.getMethod(name, paramTypes);
//        } catch (NoSuchMethodException e) {
//            return null;
//        }
//    }
//
//    private Object safeInvoke(Object target, String methodName, Object... args) {
//        try {
//            Class<?>[] paramTypes = new Class<?>[args.length];
//            for (int i = 0; i < args.length; i++) {
//                paramTypes[i] = args[i] == null ? Object.class : args[i].getClass();
//            }
//            Method m = findMethod(target.getClass(), methodName, paramTypes);
//            if (m != null) {
//                return m.invoke(target, args);
//            }
//        } catch (Exception e) {
//            log.debug("safeInvoke failed for {} on {}: {}", methodName, target.getClass().getName(), e.getMessage());
//        }
//        return null;
//    }
//
//    private Namespace[] tryListNamespacesArray() {
//        try {
//            Method m = findMethod(icebergCatalog.getClass(), "listNamespaces");
//            if (m != null) {
//                Object o = m.invoke(icebergCatalog);
//                if (o instanceof Namespace[]) {
//                    return (Namespace[]) o;
//                } else if (o instanceof Iterable) {
//                    List<Namespace> tmp = new ArrayList<>();
//                    for (Object n : (Iterable<?>) o) {
//                        if (n instanceof Namespace) tmp.add((Namespace) n);
//                    }
//                    return tmp.toArray(new Namespace[0]);
//                }
//            }
//        } catch (Exception e) {
//            log.debug("tryListNamespacesArray failed", e);
//        }
//        return null;
//    }
//}
//package com.alinesno.infra.data.lake.rest.controller;
//
//import com.alinesno.infra.data.lake.api.CatalogDto;
//import com.alinesno.infra.data.lake.rest.dto.*;
//import com.alinesno.infra.data.lake.rest.service.CatalogService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/catalogs")
//@RequiredArgsConstructor
//public class PaimonCatalogController {
//    private final CatalogService catalogService;
//
//    @PostMapping
//    public ResponseEntity<ApiResponse<Void>> createCatalog(@Valid @RequestBody CreateCatalogRequest req) {
//        catalogService.createCatalog(req);
//        return ResponseEntity.ok(ApiResponse.ok(null));
//    }
//
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<CatalogDto>>> listCatalogs() {
//        List<CatalogDto> list = catalogService.listCatalogs();
//        return ResponseEntity.ok(ApiResponse.ok(list));
//    }
//
//    @GetMapping("/{catalog}")
//    public ResponseEntity<ApiResponse<CatalogDto>> getCatalog(@PathVariable String catalog) {
//        CatalogDto dto = catalogService.getCatalog(catalog);
//        if (dto == null) return ResponseEntity.notFound().build();
//        return ResponseEntity.ok(ApiResponse.ok(dto));
//    }
//
//    @DeleteMapping("/{catalog}")
//    public ResponseEntity<ApiResponse<Void>> deleteCatalog(@PathVariable String catalog) {
//        catalogService.deleteCatalog(catalog);
//        return ResponseEntity.ok(ApiResponse.ok(null));
//    }
//
//    @PostMapping("/{catalog}/databases")
//    public ResponseEntity<ApiResponse<Void>> createDatabase(@PathVariable String catalog, @Valid @RequestBody CreateDatabaseRequest req) {
//        catalogService.createDatabase(catalog, req);
//        return ResponseEntity.ok(ApiResponse.ok(null));
//    }
//
//    @GetMapping("/{catalog}/databases")
//    public ResponseEntity<ApiResponse<List<DatabaseDto>>> listDatabases(@PathVariable String catalog) {
//        List<DatabaseDto> list = catalogService.listDatabases(catalog);
//        return ResponseEntity.ok(ApiResponse.ok(list));
//    }
//
//    @DeleteMapping("/{catalog}/databases/{db}")
//    public ResponseEntity<ApiResponse<Void>> dropDatabase(@PathVariable String catalog, @PathVariable("db") String db,
//                                                          @RequestParam(defaultValue = "false") boolean cascade) {
//        catalogService.dropDatabase(catalog, db, cascade);
//        return ResponseEntity.ok(ApiResponse.ok(null));
//    }
//
//    @PostMapping("/{catalog}/databases/{db}/tables")
//    public ResponseEntity<ApiResponse<Void>> createTable(@PathVariable String catalog, @PathVariable("db") String db,
//                                                         @Valid @RequestBody CreateTableRequest req) {
//        catalogService.createTable(catalog, db, req);
//        return ResponseEntity.ok(ApiResponse.ok(null));
//    }
//
//    @GetMapping("/{catalog}/databases/{db}/tables")
//    public ResponseEntity<ApiResponse<List<TableDto>>> listTables(@PathVariable String catalog, @PathVariable("db") String db) {
//        List<TableDto> list = catalogService.listTables(catalog, db);
//        return ResponseEntity.ok(ApiResponse.ok(list));
//    }
//
//    @GetMapping("/{catalog}/databases/{db}/tables/{table}")
//    public ResponseEntity<ApiResponse<TableDto>> getTable(@PathVariable String catalog, @PathVariable("db") String db,
//                                                          @PathVariable("table") String table) {
//        TableDto dto = catalogService.getTable(catalog, db, table);
//        if (dto == null) return ResponseEntity.notFound().build();
//        return ResponseEntity.ok(ApiResponse.ok(dto));
//    }
//
//    @DeleteMapping("/{catalog}/databases/{db}/tables/{table}")
//    public ResponseEntity<ApiResponse<Void>> dropTable(@PathVariable String catalog, @PathVariable("db") String db,
//                                                       @PathVariable("table") String table) {
//        catalogService.dropTable(catalog, db, table);
//        return ResponseEntity.ok(ApiResponse.ok(null));
//    }
//}
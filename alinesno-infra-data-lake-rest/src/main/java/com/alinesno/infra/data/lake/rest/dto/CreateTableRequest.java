//package com.alinesno.infra.data.lake.rest.dto;
//
//import com.alinesno.infra.common.web.adapter.dto.FieldDto;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotEmpty;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.util.List;
//import java.util.Map;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class CreateTableRequest {
//    @NotBlank
//    private String tableName;
//
//    private TableSchemaDto schema;
//
//    @NotEmpty
//    private List<FieldDto> fields;
//
//    private List<String> partitionKeys;
//
//    private String primaryKey;
//
//    private Map<String, String> properties;
//
//    private String comment;
//}
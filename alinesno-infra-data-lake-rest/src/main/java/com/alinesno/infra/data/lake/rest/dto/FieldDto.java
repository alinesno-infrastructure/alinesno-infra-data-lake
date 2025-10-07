//package com.alinesno.infra.data.lake.rest.dto;
//
//import jakarta.validation.constraints.NotBlank;
//import lombok.Data;
//
//@Data
//public class FieldDto {
//
//    @NotBlank
//    private String name;
//
//    @NotBlank
//    private String type; // e.g. "int", "bigint", "varchar(255)"，按 Paimon 支持的类型填写
//
//    private boolean nullable = true;
//
//    private String comment;
//
//}
//package com.alinesno.infra.data.lake.rest.dto;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class ApiResponse<T> {
//    private String code;
//    private String message;
//    private T data;
//
//    public static <T> ApiResponse<T> ok(T data) {
//        return new ApiResponse<>("OK", null, data);
//    }
//}
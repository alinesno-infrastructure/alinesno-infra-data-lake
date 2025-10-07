//package com.alinesno.infra.data.lake.rest.config;
//
//import com.aliyun.oss.OSS;
//import com.aliyun.oss.OSSClientBuilder;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//@RequiredArgsConstructor
//public class OssConfig {
//
//    @Value("${iceberg.oss.endpoint}")
//    private String endpoint;
//
//    @Value("${iceberg.oss.access-key-id}")
//    private String accessKeyId;
//
//    @Value("${iceberg.oss.access-key-secret}")
//    private String accessKeySecret;
//
//    @Bean(destroyMethod = "shutdown")
//    public OSS ossClient() {
//        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//    }
//}
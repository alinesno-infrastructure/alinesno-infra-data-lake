//package com.alinesno.infra.data.lake.rest.service;
//
//import com.aliyun.oss.OSS;
//import com.aliyun.oss.model.DeleteObjectsRequest;
//import com.aliyun.oss.model.ListObjectsRequest;
//import com.aliyun.oss.model.ObjectListing;
//import com.aliyun.oss.model.OSSObject;
//import com.aliyun.oss.model.OSSObjectSummary;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.ByteArrayInputStream;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class OssService {
//    private final OSS ossClient;
//
//    @Value("${iceberg.storage-bucket}")
//    private String bucket;
//
//    /**
//     * Put string content to OSS object with specified key.
//     */
//    public void putString(String key, String content) {
//        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
//        ossClient.putObject(bucket, key, new ByteArrayInputStream(bytes));
//    }
//
//    /**
//     * Get string content from OSS object. Returns null if object does not exist.
//     */
//    public String getString(String key) {
//        if (!ossClient.doesObjectExist(bucket, key)) {
//            return null;
//        }
//        OSSObject obj = ossClient.getObject(bucket, key);
//        try (InputStream is = obj.getObjectContent()) {
//            byte[] data = is.readAllBytes();
//            return new String(data, StandardCharsets.UTF_8);
//        } catch (Exception ex) {
//            throw new RuntimeException("read object error", ex);
//        }
//    }
//
//    /**
//     * Delete all objects under the given prefix (handles pagination).
//     */
//    public void deletePrefix(String prefix) {
//        String marker = null;
//        ObjectListing listing;
//        do {
//            ListObjectsRequest req = new ListObjectsRequest(bucket, prefix, marker, null, 1000);
//            listing = ossClient.listObjects(req);
//            List<String> keys = listing.getObjectSummaries()
//                    .stream()
//                    .map(OSSObjectSummary::getKey)
//                    .collect(Collectors.toList());
//            if (!keys.isEmpty()) {
//                DeleteObjectsRequest deleteReq = new DeleteObjectsRequest(bucket).withKeys(keys) ; //.toArray(new String[0]));
//                ossClient.deleteObjects(deleteReq);
//            }
//            marker = listing.getNextMarker();
//        } while (listing.isTruncated());
//    }
//
//    /**
//     * List all object keys that start with the given prefix (handles pagination).
//     */
//    public List<String> listChildren(String prefix) {
//        List<String> result = new ArrayList<>();
//        String marker = null;
//        ObjectListing listing;
//        do {
//            ListObjectsRequest req = new ListObjectsRequest(bucket, prefix, marker, null, 1000);
//            listing = ossClient.listObjects(req);
//            result.addAll(listing.getObjectSummaries()
//                    .stream()
//                    .map(OSSObjectSummary::getKey)
//                    .toList());
//            marker = listing.getNextMarker();
//        } while (listing.isTruncated());
//        return result;
//    }
//
//    /**
//     * Check whether an object exists.
//     */
//    public boolean exists(String key) {
//        return ossClient.doesObjectExist(bucket, key);
//    }
//
//    /**
//     * Delete a single object if it exists.
//     */
//    public void deleteObject(String key) {
//        if (ossClient.doesObjectExist(bucket, key)) {
//            ossClient.deleteObject(bucket, key);
//        }
//    }
//}
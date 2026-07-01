package com.minio.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageStatsResponse {

    private Long totalStorage;
    private String formattedTotalStorage;

    private List<BucketStorage> storagePerBucket;

    private List<UserStorage> storagePerUser;

    private List<StorageGrowth> storageGrowth;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BucketStorage {
        private String bucketName;
        private Long storageUsed;
        private String formattedStorage;
        private Long fileCount;
        private Double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStorage {
        private String username;
        private Long storageUsed;
        private String formattedStorage;
        private Long fileCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageGrowth {
        private String date;
        private Long totalStorage;
        private String formattedStorage;
        private Long fileCount;
    }

}
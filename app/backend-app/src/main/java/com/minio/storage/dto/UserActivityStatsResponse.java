package com.minio.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityStatsResponse {

    private List<UserActivity> mostActiveUsers;

    private UploadFrequency uploadFrequency;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivity {
        private String username;
        private String email;
        private Long uploadCount;
        private Long totalStorage;
        private String formattedStorage;
        private String lastUpload;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadFrequency {
        private Long today;
        private Long thisWeek;
        private Long thisMonth;
        private Long total;
        private Double avgPerDay;
        private Double avgPerWeek;
    }

}
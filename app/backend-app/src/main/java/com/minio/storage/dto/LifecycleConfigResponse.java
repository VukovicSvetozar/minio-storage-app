package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Trenutna lifecycle konfiguracija bucketa")
public class LifecycleConfigResponse {

    @Schema(description = "Naziv bucketa", example = "dokumenti")
    private String bucketName;

    @Schema(description = "Broj aktivnih pravila", example = "2")
    private int ruleCount;

    @Schema(description = "Lista lifecycle pravila")
    private List<LifecycleRuleResponse> rules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Prikaz pojedinačnog lifecycle pravila")
    public static class LifecycleRuleResponse {

        @Schema(description = "ID pravila", example = "archive-old-files")
        private String id;

        @Schema(description = "Status pravila", example = "Enabled")
        private String status;

        @Schema(description = "Prefiks filtera", example = "reports/")
        private String filterPrefix;

        @Schema(description = "Broj dana do isteka", example = "365")
        private Integer expirationDays;

        @Schema(description = "Broj dana do tranzicije", example = "90")
        private Integer transitionDays;

        @Schema(description = "Ciljni bucket za tranziciju (MinIO-specifično - u AWS S3 ovaj parametar označava storage klasu)",
                example = "archive-bucket")
        private String transitionBucket;
    }

}
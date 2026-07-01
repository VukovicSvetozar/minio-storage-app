package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Statistika zauzeća prostora po pojedinačnom bucketu")
public class BucketStatisticsResponse {

    @Schema(description = "Naziv bucketa", example = "slike-projekti")
    private String bucketName;

    @Schema(description = "Ukupan broj fajlova u bucketu", example = "320")
    private long fileCount;

    @Schema(description = "Ukupna veličina svih fajlova u bucketu u bajtovima", example = "2147483648")
    private long totalSizeBytes;

    @Schema(description = "Ljudima čitljiva ukupna veličina", example = "2.0 GB")
    private String totalSizeFormatted;

}
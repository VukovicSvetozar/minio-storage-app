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
@Schema(description = "Informacije o statusu verzioniranja na nivou bucketa")
public class VersioningStatusResponse {

    @Schema(description = "Naziv bucketa", example = "dokumentacija")
    private String bucketName;

    @Schema(description = "Da li je verzioniranje trenutno aktivno", example = "true")
    private boolean versioningEnabled;

    @Schema(description = "Sirovi MinIO status (ENABLED, SUSPENDED, OFF)", example = "ENABLED")
    private String status;

}
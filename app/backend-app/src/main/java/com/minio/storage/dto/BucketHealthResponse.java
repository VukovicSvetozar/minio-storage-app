package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rezultat provjere zdravlja bucketa")
public class BucketHealthResponse {

    @Schema(description = "Naziv bucketa", example = "moje-slike")
    private String bucketName;

    @Schema(description = "Bucket postoji i aktivan je u bazi", example = "true")
    private boolean existsInDatabase;

    @Schema(description = "Bucket postoji na MinIO serveru", example = "true")
    private boolean existsInStorage;

    @Schema(description = "MinIO server je dostupan i odgovara", example = "true")
    private boolean storageReachable;

    @Schema(description = "Opšta ocjena - true samo ako su svi uslovi ispunjeni", example = "true")
    private boolean healthy;

    @Schema(
            description = "Kratki kod greške za automatizaciju i monitoring",
            example = "MISSING_IN_DB",
            allowableValues = {
                    "MISSING_IN_DB",
                    "MISSING_IN_STORAGE",
                    "NOT_FOUND_ANYWHERE",
                    "STORAGE_UNREACHABLE",
                    "PERMISSION_DENIED"
            }
    )
    private String errorCode;

    @Schema(description = "Poruka s pojašnjenjem stanja ili opisa greške")
    private String message;

    @Schema(description = "Vrijeme izvršavanja provjere")
    private LocalDateTime checkedAt;

}
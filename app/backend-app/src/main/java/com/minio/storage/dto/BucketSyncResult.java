package com.minio.storage.dto;

import com.minio.storage.enums.BucketSyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rezultat obrade pojedinačnog bucketa tokom procesa sinhronizacije")
public class BucketSyncResult {

    @Schema(description = "Naziv bucketa koji je obrađivan", example = "projekat-2024")
    private String bucketName;

    @Schema(description = "Status ishoda sinhronizacije", example = "IMPORTED")
    private BucketSyncStatus status;

    @Schema(description = "Broj fajlova (može biti null ako je bucket preskočen)", example = "150")
    private Long fileCount;

    @Schema(description = "Ukupna veličina u bajtovima (može biti null ako je bucket preskočen)", example = "1073741824")
    private Long totalSize;

    @Schema(description = "Trenutno stanje javne dostupnosti bucketa", example = "true")
    private boolean isPublic;

}
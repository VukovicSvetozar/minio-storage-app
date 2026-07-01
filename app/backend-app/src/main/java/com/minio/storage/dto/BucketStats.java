package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Tehnički statistički podaci o sadržaju skladišnog prostora")
public class BucketStats {

    @Schema(description = "Naziv bucketa na koji se statistika odnosi", example = "moje-slike")
    private String bucketName;

    @Schema(description = "Ukupan broj objekata/fajlova u bucketu", example = "42")
    private long fileCount;

    @Schema(description = "Ukupna zauzeta veličina u bajtovima", example = "1048576")
    private long totalSize;

}
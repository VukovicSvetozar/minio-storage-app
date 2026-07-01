package com.minio.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "Sažeti prikaz verzije fajla za listu")
public class FileVersionSummaryResponse {

    @Schema(description = "Interni DB identifikator verzije - koristi se u URL-u", example = "10")
    private Long id;

    @Schema(description = "MinIO UUID identifikator verzije - samo informacija", example = "abc-123-def-456")
    private String minioVersionId;

    @Schema(description = "Redni broj verzije", example = "3")
    private Integer versionNumber;

    @Schema(description = "Veličina verzije u bajtovima", example = "1024000")
    private Long size;

    @Schema(description = "Datum i vrijeme kreiranja verzije")
    private LocalDateTime uploadedAt;

    @Schema(description = "Korisnik koji je kreirao verziju", example = "marko")
    private String uploadedBy;

    @JsonProperty("isLatest")
    @Schema(description = "Da li je ovo trenutno najnovija verzija", example = "true")
    private boolean isLatest;

}
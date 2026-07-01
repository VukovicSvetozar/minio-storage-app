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
@Schema(description = "Detaljne informacije o specifičnoj verziji fajla")
public class FileVersionDetailResponse {

    @Schema(description = "Interni DB identifikator verzije", example = "10")
    private Long id;

    @Schema(description = "ID fajla kojem verzija pripada", example = "123")
    private Long fileId;

    @Schema(description = "Originalni naziv fajla", example = "dokument.pdf")
    private String fileName;

    @Schema(description = "MinIO UUID identifikator verzije", example = "abc-123-def-456")
    private String minioVersionId;

    @Schema(description = "Redni broj verzije", example = "3")
    private Integer versionNumber;

    @Schema(description = "Veličina u bajtovima", example = "1048576")
    private Long size;

    @Schema(description = "Putanja u objektnom skladištenju (MinIO)", example = "user-1/fajl_123_abc")
    private String storageKey;

    @Schema(description = "Kontrolni zbir (Checksum) fajla", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String checksum;

    @Schema(description = "Vrijeme kreiranja verzije")
    private LocalDateTime uploadedAt;

    @Schema(description = "Korisnik koji je kreirao verziju", example = "marko")
    private String uploadedBy;

    @JsonProperty("isLatest")
    @Schema(description = "Da li je ovo trenutno aktivna verzija", example = "false")
    private boolean isLatest;

}
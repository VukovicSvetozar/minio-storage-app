package com.minio.storage.dto;

import com.minio.storage.enums.FileCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za izmjenu metapodataka postojećeg fajla")
public class UpdateFileMetadataRequest {

    @Size(max = 1000, message = "Opis ne može biti duži od 1000 karaktera")
    @Schema(description = "Novi opis fajla", example = "Ažurirana verzija projektne dokumentacije")
    private String description;

    @Schema(description = "Niz tagova za lakšu pretragu i grupisanje", example = "[\"projekat\", \"2026\", \"pdf\"]")
    private String[] tags;

    @Schema(description = "Nova kategorija fajla", example = "DOCUMENT")
    private FileCategory category;

    @Schema(description = "Promjena statusa javne dostupnosti (true = javno, false = privatno)", example = "true")
    private Boolean isPublic;

}
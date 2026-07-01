package com.minio.storage.dto;

import com.minio.storage.enums.FileCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Parametri za naprednu pretragu i filtriranje fajlova")
public class FileSearchRequest {

    @Schema(description = "Pojam za pretragu koji se provjerava kroz naziv, opis i tagove", example = "izvještaj")
    private String searchTerm;

    @Schema(description = "Naziv specifičnog bucketa u kojem se vrši pretraga", example = "dokumenti-2026")
    private String bucketName;

    @Schema(description = "Kategorija fajla po kojoj se filtrira", example = "DOCUMENT")
    private FileCategory category;

    @Schema(description = "MIME tip sadržaja za filtriranje", example = "application/pdf")
    private String contentType;

    @Schema(description = "Filter po javnoj dostupnosti (sigurnosna pravila imaju prioritet)", example = "true")
    private Boolean isPublic;

    @Schema(description = "Korisničko ime osobe koja je otpremila fajl", example = "vuk_admin")
    private String uploadedBy;

    @Schema(description = "Filtriraj fajlove otpremljene nakon ovog datuma", example = "2026-01-01T00:00:00")
    private LocalDateTime uploadedAfter;

    @Schema(description = "Filtriraj fajlove otpremljene prije ovog datuma", example = "2026-12-31T23:59:59")
    private LocalDateTime uploadedBefore;

    @Schema(description = "Minimalna veličina fajla u bajtovima", example = "1024")
    private Long minSize;

    @Schema(description = "Maksimalna veličina fajla u bajtovima", example = "10485760")
    private Long maxSize;

}
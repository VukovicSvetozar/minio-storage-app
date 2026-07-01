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
@Schema(description = "Odgovor koji sadrži metapodatke o fajlu i privremeni presigned URL za preuzimanje")
public class ShareLinkAccessResponse {

    @Schema(description = "Originalni naziv fajla", example = "dokument.pdf")
    private String originalFileName;

    @Schema(description = "MIME tip fajla", example = "application/pdf")
    private String contentType;

    @Schema(description = "Veličina fajla u bajtovima", example = "1048576")
    private Long fileSize;

    @Schema(description = "Formatirana veličina fajla čitljiva ljudima", example = "1.00 MB")
    private String formattedSize;

    @Schema(description = "MinIO presigned URL za direktno preuzimanje fajla (važi 5 minuta)", example = "http://localhost:9000/bucket/...")
    private String downloadUrl;

    @Schema(description = "Opis fajla ili linka", example = "Javni link za klijente")
    private String description;

    @Schema(description = "Korisničko ime osobe koja je podijelila fajl", example = "vuk_it")
    private String sharedBy;

    @Schema(description = "Trenutni broj pristupa ovom linku", example = "12")
    private Integer accessCount;

}
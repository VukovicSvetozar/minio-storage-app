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
@Schema(description = "Informacije o generisanom linku za javno dijeljenje fajla")
public class ShareLinkResponse {

    @Schema(description = "Jedinstveni identifikator share linka u bazi", example = "101")
    private Long id;

    @Schema(description = "Jedinstveni bezbjednosni token za pristup", example = "a1b2c3d4e5f6g7h8")
    private String shareToken;

    @Schema(description = "Puni URL koji se šalje krajnjem korisniku", example = "http://localhost:8082/api/share/a1b2c3d4e5f6g7h8")
    private String shareUrl;

    @Schema(description = "ID fajla na koji se link odnosi", example = "42")
    private Long fileId;

    @Schema(description = "Originalni naziv fajla prilikom otpremanja", example = "diplomski_rad_v2.pdf")
    private String originalFileName;

    @Schema(description = "Korisničko ime osobe koja je kreirala link", example = "marko_it")
    private String createdBy;

    @Schema(description = "Datum i vrijeme kreiranja linka")
    private LocalDateTime createdAt;

    @Schema(description = "Datum i vrijeme kada link prestaje da važi")
    private LocalDateTime expiresAt;

    @Schema(description = "Preostalo vrijeme trajanja u čitljivom formatu", example = "2 dana preostalo")
    private String timeRemaining;

    @Schema(description = "Ukupan broj pristupa putem ovog linka", example = "5")
    private int accessCount;

    @Schema(description = "Datum i vrijeme posljednjeg pristupa putem ovog linka")
    private LocalDateTime lastAccessedAt;

    @Schema(description = "Indikator da li je link ručno deaktiviran", example = "true")
    private boolean isActive;

    @Schema(description = "Indikator da li je link istekao na osnovu vremena", example = "false")
    private boolean isExpired;

    @Schema(description = "Opcioni opis ili napomena uz share link", example = "Link za kolege sa projekta")
    private String description;

}
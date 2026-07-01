package com.minio.storage.dto;

import com.minio.storage.enums.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detaljan prikaz zapisa o aktivnosti u sistemu")
public class ActivityLogResponse {

    @Schema(description = "Jedinstveni identifikator zapisa u dnevniku", example = "1024")
    private Long id;

    @Schema(description = "Tip izvršene aktivnosti (npr. LOGIN, FILE_UPLOAD, BUCKET_CREATE)")
    private ActivityType activityType;

    @Schema(description = "Korisničko ime osobe koja je izvršila aktivnost", example = "vuk_admin")
    private String username;

    @Schema(description = "Kratak opis aktivnosti", example = "Korisnik je otpremio fajl izvjestaj.pdf")
    private String description;

    @Schema(description = "Naziv entiteta/resursa nad kojim je akcija izvršena", example = "FileMetadata")
    private String resourceType;

    @Schema(description = "ID resursa nad kojim je akcija izvršena", example = "55")
    private Long resourceId;

    @Schema(description = "Naziv resursa (npr. ime fajla ili bucketa)", example = "izvjestaj.pdf")
    private String resourceName;

    @Schema(description = "Dodatni tehnički detalji o operaciji (često u JSON formatu)")
    private String details;

    @Schema(description = "IP adresa sa koje je upućen zahtjev", example = "192.168.1.15")
    private String ipAddress;

    @Schema(description = "Datum i vrijeme kada se aktivnost dogodila")
    private LocalDateTime timestamp;

    @Schema(description = "Indikator uspješnosti operacije", example = "true")
    private Boolean success;

    @Schema(description = "Poruka o grešci ukoliko operacija nije uspjela", example = "Skladište je puno")
    private String errorMessage;

}
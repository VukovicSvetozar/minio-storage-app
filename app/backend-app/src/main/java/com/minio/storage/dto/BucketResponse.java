package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detaljne informacije o skladišnom prostoru (bucketu)")
public class BucketResponse {

    @Schema(description = "Jedinstveni identifikator zapisa u bazi", example = "1")
    private Long id;

    @Schema(description = "Jedinstveni naziv bucketa na MinIO serveru", example = "moje-slike")
    private String name;

    @Schema(description = "Indikator da li je bucket javan (dostupan bez autentifikacije)", example = "false")
    private boolean isPublic;

    @Schema(description = "Indikator da li je dozvoljeno javno (anonimno) otpremanje fajlova", example = "false")
    private boolean allowPublicUpload;

    @Schema(description = "Korisničko ime vlasnika bucketa (ili 'system' za uvezene)", example = "vuk_admin")
    private String owner;

    @Schema(description = "Opis namjene i sadržaja skladišnog prostora", example = "Skladište za PDF fakture")
    private String description;

    @Schema(description = "Trenutni broj fajlova u bucketu", example = "42")
    private Long fileCount;

    @Schema(description = "Ukupna veličina svih fajlova izražena u bajtovima", example = "1048576")
    private Long totalSize;

    @Schema(description = "Maksimalna dozvoljena kvota prostora u bajtovima", example = "10737418240")
    private Long maxSizeBytes;

    @Schema(description = "Datum i vrijeme kada je bucket registrovan u sistemu")
    private LocalDateTime createdAt;

    @Schema(description = "Datum i vrijeme posljednje promjene metapodataka")
    private LocalDateTime lastModifiedAt;

    @Schema(description = "Korisničko ime osobe koja je izvršila posljednju izmjenu")
    private String lastModifiedBy;

    @Schema(description = "Vrijeme posljednje uspješne sinhronizacije statistike sa storage serverom")
    private LocalDateTime lastSyncAt;

    @Schema(description = "Da li je verzioniranje fajlova uključeno za ovaj bucket", example = "false")
    private boolean versioningEnabled;

    @Schema(description = "Da li bucket podržava Object Lock i Retention Policy", example = "false")
    private boolean objectLockEnabled;

}
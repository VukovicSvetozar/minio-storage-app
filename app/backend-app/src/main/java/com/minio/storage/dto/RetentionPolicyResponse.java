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
@Schema(description = "Trenutna retention konfiguracija bucketa")
public class RetentionPolicyResponse {

    @Schema(description = "Naziv bucketa", example = "dokumenti")
    private String bucketName;

    @Schema(description = "Da li je Object Lock uključen na bucketu", example = "true")
    private boolean objectLockEnabled;

    @Schema(description = "Mod zadržavanja", example = "GOVERNANCE")
    private String mode;

    @Schema(description = "Trajanje zadržavanja u danima", example = "365")
    private Integer days;

    @Schema(
            description = "Napomena o pravilima primijenjenog moda zaštite",
            example = "GOVERNANCE mod dozvoljava administratorima sa posebnim privilegijama da zaobiđu zaštitu. " +
                    "COMPLIANCE mod je strogo restriktivan - brisanje objekata je onemogućeno svim korisnicima, uključujući i administraciju, " +
                    "do isteka perioda zadržavanja."
    )
    private String note;

}
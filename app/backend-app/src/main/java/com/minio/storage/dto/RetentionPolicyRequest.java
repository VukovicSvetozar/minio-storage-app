package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Zahtjev za postavljanje pravila zadržavanja (Retention Policy) na bucket")
public class RetentionPolicyRequest {

    @NotNull(message = "Režim zadržavanja je obavezan.")
    @Schema(
            description = "Režim zadržavanja: COMPLIANCE (stroga zaštita) ili GOVERNANCE (fleksibilna zaštita uz privilegije)",
            example = "GOVERNANCE",
            allowableValues = {"COMPLIANCE", "GOVERNANCE"}
    )
    private String mode;

    @NotNull(message = "Trajanje zadržavanja je obavezno.")
    @Min(value = 1, message = "Trajanje mora biti najmanje 1 dan.")
    @Schema(description = "Broj dana tokom kojih se objekti ne mogu brisati", example = "365")
    private Integer days;

}
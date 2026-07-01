package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za kreiranje novog linka za javno dijeljenje fajla")
public class CreateShareLinkRequest {

    @Schema(
            description = "Vrijeme trajanja linka izraženo u satima. Nakon ovog perioda link postaje nevažeći.",
            example = "24",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Broj sati isteka je obavezan")
    @Min(value = 1, message = "Minimalno trajanje linka je 1 sat")
    private Integer expirationHours;

    @Schema(
            description = "Opcioni opis ili napomena o svrsi kreiranja ovog share linka",
            example = "Link za spoljne saradnike na projektu",
            nullable = true
    )
    private String description;

}
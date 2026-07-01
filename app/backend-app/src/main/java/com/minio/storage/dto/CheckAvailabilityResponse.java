package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Univerzalni odgovor za provjeru dostupnosti resursa (korisničko ime, email, naziv bucketa)")
public class CheckAvailabilityResponse {

    @Schema(description = "Indikator da li je traženi podatak slobodan za korištenje (true ako jeste)", example = "true")
    private boolean available;

    @Schema(description = "Originalna vrijednost koja je poslata na provjeru", example = "vuk123")
    private String value;

}
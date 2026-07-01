package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Odgovor sa novim Access Tokenom nakon osvježavanja sesije")
public class RefreshTokenResponse {

    @Schema(description = "Novi JWT pristupni token", example = "eyJhbGciOiJIUzI1...")
    private String accessToken;

    @Schema(description = "Postojeći ili novi token za osvježavanje", example = "d3b07384-...")
    private String refreshToken;

    @Schema(description = "Tip tokena", example = "Bearer")
    @Builder.Default
    private String type = "Bearer";

    @Schema(description = "Vrijeme trajanja novog Access Tokena u sekundama", example = "3600")
    private Long expiresIn;

}
package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za generisanje novog Access Tokena pomoću Refresh Tokena")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token je obavezan")
    @Schema(description = "Važeći refresh token dobijen prilikom prijave ili prethodnog osvježavanja")
    private String refreshToken;

}
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
@Schema(description = "Odgovor sa podacima o sesiji, korisniku i pristupnim tokenima nakon uspješne autentifikacije")
public class AuthResponse {

    @Schema(description = "JWT pristupni token (Access Token) za autorizaciju zahtjeva",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Token za osvježavanje sesije (Refresh Token)",
            example = "d3b07384-d99e-439a-9c20-...")
    private String refreshToken;

    @Schema(description = "Tip tokena", example = "Bearer")
    @Builder.Default
    private String type = "Bearer";

    @Schema(description = "Jedinstveni identifikator korisnika u bazi", example = "1")
    private Long id;

    @Schema(description = "Korisničko ime", example = "vuk_pro")
    private String username;

    @Schema(description = "E-mail adresa korisnika", example = "vuk@example.com")
    private String email;

    @Schema(description = "Glavna uloga korisnika u sistemu", example = "ROLE_ADMIN")
    private String role;

    @Schema(description = "Vrijeme trajanja tokena izraženo u sekundama", example = "3600")
    private Long expiresIn;

    @Schema(description = "Indikator da li je korisnik potvrdio svoju email adresu", example = "true")
    private boolean emailVerified;

}
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
@Schema(description = "Osnovni podaci o korisničkom nalogu")
public class UserResponse {

    @Schema(description = "Jedinstveni identifikator korisnika", example = "1")
    private Long id;

    @Schema(description = "Korisničko ime", example = "vuk_pro")
    private String username;

    @Schema(description = "E-mail adresa korisnika", example = "vuk@example.com")
    private String email;

    @Schema(description = "Uloga korisnika u sistemu", example = "USER", allowableValues = {"USER", "ADMIN"})
    private String role;

    @Schema(description = "Indikator da li je nalog aktivan i može se koristiti", example = "true")
    private Boolean isActive;

}
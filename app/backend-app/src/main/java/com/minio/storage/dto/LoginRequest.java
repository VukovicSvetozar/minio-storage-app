package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Podaci za prijavu na sistem")
public class LoginRequest {

    @NotBlank(message = "Korisničko ime ili email su obavezni")
    @Schema(example = "korisnik_pro", description = "Unesite svoje korisničko ime ili email adresu")
    private String usernameOrEmail;

    @NotBlank(message = "Lozinka je obavezna")
    @Schema(example = "Lozinka123*", description = "Lozinka povezana sa vašim nalogom")
    private String password;

//    @Schema(hidden = true)
    @SuppressWarnings("unused")
    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail != null ? usernameOrEmail.trim() : null;
    }

}
package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za resetovanje lozinke putem email adrese")
public class ForgotPasswordRequest {

    @NotBlank(message = "Email adresa je obavezna")
    @Email(message = "Format email adrese nije ispravan")
    @Size(max = 100, message = "Email adresa ne može biti duža od 100 karaktera")
    @Schema(example = "korisnik@example.com", description = "Email povezan sa vašim nalogom na koji će biti poslat token")
    private String email;

}
package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za verifikaciju email adrese putem ključa")
public class VerificationRequest {

    @NotBlank(message = "Korisničko ime je obavezno")
    @Size(min = 3, max = 30, message = "Korisničko ime mora imati između 3 i 30 karaktera")
    @Pattern(regexp = "^[a-z0-9_]*$", message = "Korisničko ime može sadržiti samo mala slova, brojeve i donje crte")
    @Schema(example = "korisnik_pro", description = "Korisničko ime korisnika koji vrši verifikaciju")
    private String username;

    @NotBlank(message = "Verifikacioni ključ je obavezan")
    @Size(min = 12, max = 12, message = "Verifikacioni ključ mora imati tačno 12 karaktera")
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "Verifikacioni ključ može sadržiti samo slova i brojeve")
    @Schema(example = "aB3xK9mP2qRt", description = "Jedinstveni ključ primljen putem email poruke")
    private String key;

}
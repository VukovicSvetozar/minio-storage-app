package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Podaci za registraciju novog korisnika")
public class RegisterRequest {

    @NotBlank(message = "Korisničko ime je obavezno")
    @Size(min = 3, max = 30, message = "Korisničko ime mora imati između 3 i 30 karaktera")
    @Pattern(regexp = "^[a-z0-9_]*$", message = "Korisničko ime može sadržiti samo mala slova, brojeve i donje crte")
    @Schema(example = "korisnik_pro", description = "Jedinstveno korisničko ime koje se koristi za prijavu")
    private String username;

    @NotBlank(message = "Email adresa je obavezna")
    @Email(message = "Format email adrese nije ispravan")
    @Size(max = 100, message = "Email adresa ne može biti duža od 100 karaktera")
    @Schema(example = "korisnik@example.com", description = "Email adresa za verifikaciju i obavještenja")
    private String email;

    @NotBlank(message = "Lozinka je obavezna")
    @Size(min = 8, max = 100, message = "Lozinka mora imati između 8 i 100 karaktera")
    @Pattern(regexp = "^(?!.*[<>\"'\\\\/])(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&*_=+\\-])(?=\\S+$).{8,}$",
            message = "Lozinka mora sadržiti bar jedno veliko slovo, broj i specijalni znak")
    @Schema(example = "Lozinka123*", description = "Sigurna lozinka (minimalno 8 karaktera, veliko slovo, broj i specijalan znak)")
    private String password;

}
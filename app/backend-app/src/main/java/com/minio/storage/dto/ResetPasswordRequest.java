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
@Schema(description = "Zahtjev za postavljanje nove lozinke putem sigurnosnog tokena")
public class ResetPasswordRequest {

    @NotBlank(message = "Token je obavezan")
    @Schema(description = "Sigurnosni token koji ste dobili putem email poruke")
    private String token;

    @NotBlank(message = "Nova lozinka je obavezna")
    @Size(min = 8, max = 100, message = "Lozinka mora imati između 8 i 100 karaktera")
    @Pattern(regexp = "^(?!.*[<>\"'\\\\/])(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&*_=+\\-])(?=\\S+$).{8,}$",
            message = "Lozinka mora sadržiti bar jedno veliko slovo, broj i specijalni znak")
    @Schema(example = "NovaLozinka123!", description = "Nova lozinka koja mora ispunjavati iste sigurnosne uslove kao pri registraciji")
    private String newPassword;

}
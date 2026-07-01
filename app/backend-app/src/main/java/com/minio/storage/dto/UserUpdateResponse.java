package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Odgovor servera nakon administrativne izmjene korisnika")
public class UserUpdateResponse {

    @Schema(description = "Opisna poruka o rezultatu akcije", example = "Uloga korisnika je uspješno ažurirana.")
    private String message;

    @Schema(description = "Ažurirani podaci o korisniku")
    private UserResponse user;

}
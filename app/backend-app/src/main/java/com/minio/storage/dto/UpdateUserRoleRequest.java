package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {

    @Schema(
            description = "Nova uloga koja se dodjeljuje korisniku",
            example = "ADMIN",
            allowableValues = {"USER", "ADMIN"}
    )
    @NotBlank(message = "Uloga je obavezna stavka.")
    @Pattern(
            regexp = "^(USER|ADMIN)$",
            message = "Neispravna uloga. Dozvoljene vrijednosti su: USER ili ADMIN."
    )
    private String role;

}
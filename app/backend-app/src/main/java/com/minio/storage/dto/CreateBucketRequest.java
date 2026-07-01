package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Podaci za kreiranje novog bucketa (skladišnog prostora)")
public class CreateBucketRequest {

    @NotBlank(message = "Naziv bucketa je obavezan")
    @Size(min = 3, max = 63, message = "Naziv mora imati između 3 i 63 karaktera")
    @Pattern(
            regexp = "^[a-z0-9](?!.*\\.\\.)[a-z0-9.-]{1,61}[a-z0-9]$",
            message = "Naziv može sadržavati mala slova, brojeve, tačke i crtice. Ne može početi ili završiti tačkom ili crticom niti sadržavati dvije uzastopne tačke."
    )
    @Schema(example = "projekat-dokumentacija", description = "Jedinstveni naziv bucketa prema S3 standardu")
    private String bucketName;

    @Schema(description = "Određuje da li je sadržaj bucketa dostupan svima", example = "false", defaultValue = "false")
    private boolean isPublic;

    @Schema(description = "Određuje da li anonimni korisnici mogu otpremati fajlove", example = "false", defaultValue = "false")
    private boolean allowPublicUpload;

    @Size(max = 500, message = "Opis ne smije preći 500 karaktera")
    @Schema(example = "Skladište za PDF dokumente klijenata", description = "Namjena ovog skladišnog prostora")
    private String description;

    @NotNull(message = "Maksimalna kvota (maxSizeGb) je obavezna")
    @Min(value = 1, message = "Minimalna dozvoljena kvota je 1 GB")
    @Max(value = 1000, message = "Maksimalna dozvoljena kvota je 1000 GB")
    @Schema(example = "10", description = "Maksimalni kapacitet bucketa izražen u gigabajtima")
    private Long maxSizeGb;

    @Schema(
            description = "Omogućava Object Lock podršku za retention policy. " +
                    "Može se postaviti samo pri kreiranju bucketa - ne može se dodati naknadno.",
            example = "false",
            defaultValue = "false"
    )
    private boolean objectLock = false;

}
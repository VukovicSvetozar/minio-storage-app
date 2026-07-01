package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za promjenu statusa verzioniranja")
public class VersioningUpdateBucketRequest {

    @NotNull(message = "Polje 'enabled' je obavezno.")
    @Schema(description = "Da li uključiti (true) ili pauzirati (false) verzioniranje", example = "true")
    private boolean enabled;

}
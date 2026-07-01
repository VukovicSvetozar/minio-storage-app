package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za izmjenu polise pristupa i vidljivosti bucketa")
public class UpdateBucketPolicyRequest {

    @NotNull(message = "Status javnosti (isPublic) je obavezan")
    @Schema(description = "Postavlja bucket kao javan (true) ili privatan (false)", example = "true")
    private Boolean isPublic;

    @Schema(description = "Opciona izmjena dozvole za javno otpremanje (ako se pošalje null, status ostaje nepromijenjen)", example = "false")
    private Boolean allowPublicUpload;

}
package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Podaci o generisanom privremenom linku za pristup fajlu")
public class PresignedUrlResponse {

    @Schema(description = "Privremeni URL za direktno preuzimanje ili pregled fajla",
            example = "https://minio.example.com/bucket/file.jpg?X-Amz-Algorithm=...")
    private String url;

    @Schema(description = "Period važenja linka u sekundama", example = "7200")
    private Integer expiresIn;

    @Schema(description = "Tačno vrijeme kada link prestaje da važi (UTC format)")
    private Instant expiresAt;

}
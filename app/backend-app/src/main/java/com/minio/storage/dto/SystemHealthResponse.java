package com.minio.storage.dto;

import com.minio.storage.enums.HealthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detaljan izvještaj o zdravlju sistema")
public class SystemHealthResponse {

    @Schema(description = "Ukupni status sistema", example = "UP")
    private HealthStatus status;

    @Schema(description = "Vrijeme izvršenja provjere")
    private LocalDateTime timestamp;

    @Schema(description = "Verzija aplikacije", example = "1.0.0")
    private String version;

    @Schema(description = "Uptime aplikacije u sekundama", example = "3600")
    private Long uptimeSeconds;

    @Schema(description = "Status baze podataka")
    private ComponentHealth database;

    @Schema(description = "Status MinIO skladišta")
    private ComponentHealth minioStorage;

    @Schema(description = "Status runtime okruženja aplikacije")
    private ComponentHealth application;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Informacije o zdravlju pojedinačne komponente")
    public static class ComponentHealth {
        @Schema(description = "Status komponente", example = "UP")
        private HealthStatus status;

        @Schema(description = "Poruka o stanju", example = "Baza podataka je dostupna")
        private String message;

        @Schema(description = "Vrijeme odziva u milisekundama", example = "12")
        private Long responseTimeMs;

        @Schema(description = "Dodatni dijagnostički detalji")
        private Map<String, Object> details;
    }

}
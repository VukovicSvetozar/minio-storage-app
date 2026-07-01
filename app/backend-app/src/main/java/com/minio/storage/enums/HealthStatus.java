package com.minio.storage.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status komponente sistema")
public enum HealthStatus {
    @Schema(description = "Komponenta radi ispravno")
    UP,
    @Schema(description = "Komponenta radi, ali sa poteškoćama")
    DEGRADED,
    @Schema(description = "Komponenta nije u funkciji")
    DOWN
}
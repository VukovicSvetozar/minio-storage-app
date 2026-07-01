package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za promjenu vidljivosti fajla")
public class UpdateFileVisibilityRequest {

    @NotNull(message = "Polje isPublic je obavezno.")
    @Schema(description = "true = javno, false = privatno", example = "true")
    private Boolean isPublic;
}
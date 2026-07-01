package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standardni odgovor sa porukom o uspjehu")
public class MessageResponse {

    @Schema(example = "Akcija je uspješno izvršena")
    private String message;

}
package com.minio.storage.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@Schema(description = "Standardni format greške u aplikaciji")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Schema(description = "Vrijeme kada je greška nastala", example = "2026-01-10T10:15:30")
    private String timestamp;

    @Schema(description = "HTTP status kod", example = "400")
    private int status;

    @Schema(description = "Kratki naziv greške", example = "Bad Request")
    private String error;

    @Schema(description = "Detaljna poruka za korisnika", example = "Došlo je do greške pri obradi zahtjeva")
    private String message;

    @Schema(description = "Putanja na kojoj je nastala greška", example = "/api/endpoint")
    private String path;

    @Schema(description = "Greške po poljima (samo za validacijske greške)")
    private Map<String, String> fieldErrors;

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(
                LocalDateTime.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                null
        );
    }
}
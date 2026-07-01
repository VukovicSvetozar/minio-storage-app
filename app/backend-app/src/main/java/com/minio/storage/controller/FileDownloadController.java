package com.minio.storage.controller;

import com.minio.storage.dto.PresignedUrlResponse;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Preuzimanje Fajlova", description = "Rute za generisanje linkova i direktno preuzimanje")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@SuppressWarnings("unused")
public class FileDownloadController {

    private final FileService fileService;

    @Operation(
            summary = "Generisanje linka za preuzimanje (Download)",
            description = "Kreira privremeni presigned URL za direktno preuzimanje fajla sa skladišta. " +
                    "Forsira preuzimanje fajla na disk (Content-Disposition: attachment)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Link uspješno generisan"),
            @ApiResponse(responseCode = "403", description = "Nemate pravo preuzimanja ovog fajla",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/{id:\\d+}/download-link")
    public ResponseEntity<PresignedUrlResponse> generateDownloadLink(
            @Parameter(description = "ID fajla", example = "42")
            @PathVariable Long id,
            @Parameter(description = "Trajanje linka u sekundama (max 7200, podrazumijevano 900)", example = "3600")
            @RequestParam(required = false) Integer expiresInSeconds,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(fileService.generateDownloadLink(id, expiresInSeconds, request));
    }

    @Operation(
            summary = "Generisanje linka za pregled (Preview)",
            description = "Kreira privremeni presigned URL za direktan pregled fajla u pregledaču (Content-Disposition: inline). " +
                    "Idealno za prikaz slika, PDF dokumenata i video zapisa bez prisilnog preuzimanja."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Link uspješno generisan"),
            @ApiResponse(responseCode = "403", description = "Nemate pravo pregleda ovog fajla",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/{id:\\d+}/preview-link")
    public ResponseEntity<PresignedUrlResponse> generatePreviewLink(
            @Parameter(description = "ID fajla", example = "42")
            @PathVariable Long id,
            @Parameter(description = "Trajanje linka u sekundama (max 7200, podrazumijevano 900)", example = "3600")
            @RequestParam(required = false) Integer expiresInSeconds,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(fileService.generatePreviewLink(id, expiresInSeconds, request));
    }

    @Operation(
            summary = "Direktno preuzimanje fajla (Proxy Stream)",
            description = "Preuzima fajl sa MinIO servera i streamuje ga klijentu kroz backend. " +
                    "Ovaj pristup krije MinIO URL i omogućava potpunu kontrolu nad HTTP zaglavljima."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fajl uspješno preuzet"),
            @ApiResponse(responseCode = "403", description = "Nemate pravo pristupa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/{id:\\d+}/download")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "ID fajla", example = "42")
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return fileService.streamFile(id, request);
    }

}
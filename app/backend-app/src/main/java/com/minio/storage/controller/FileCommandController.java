package com.minio.storage.controller;

import com.minio.storage.dto.*;
import com.minio.storage.enums.FileCategory;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.FileService;
import com.minio.storage.util.ActivityLogHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Upravljanje Fajlovima", description = "Rute za upload, brisanje i izmjenu fajlova")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@SuppressWarnings("unused")
public class FileCommandController {

    private final FileService fileService;
    private final ActivityLogHelper activityLogHelper;

    @Operation(
            summary = "Upload fajla u bucket",
            description = "Otprema fajl u specifični bucket sa metadata podacima. Automatski provjerava kvotu i prava pristupa."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Fajl uspješno otpremljen"),
            @ApiResponse(responseCode = "400", description = "Nevažeći zahtjev ili bucket nema dovoljno prostora",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Nemate pravo upload-a u ovaj bucket",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bucket ne postoji",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/buckets/{bucketName}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "Naziv bucketa u koji se otprema fajl", example = "moje-slike")
            @PathVariable String bucketName,

            @Parameter(description = "Fajl za upload (max 100MB)")
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Opis fajla (opciono)", example = "Slika sa odmora")
            @RequestParam(required = false) String description,

            @Parameter(description = "Tagovi odvojeni zarezom (opciono)", example = "odmor,more,2026")
            @RequestParam(required = false) String tags,

            @Parameter(description = "Kategorija fajla", example = "IMAGE")
            @RequestParam(required = false) FileCategory category,

            @Parameter(description = "Da li je fajl javan", example = "false")
            @RequestParam(required = false, defaultValue = "false") boolean isPublic,

            HttpServletRequest request
    ) {
        FileUploadResponse response = fileService.uploadFile(
                bucketName,
                file,
                description,
                tags,
                category,
                isPublic,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Brisanje fajla",
            description = "Fizički briše fajl sa MinIO servera i metadata iz baze. Ova akcija se ne može poništiti."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fajl uspješno obrisan"),
            @ApiResponse(responseCode = "403", description = "Nemate pravo brisanja ovog fajla",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/files/{id}")
    public ResponseEntity<MessageResponse> deleteFile(
            @Parameter(description = "ID fajla za brisanje", example = "42")
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        fileService.deleteFile(id, request);
        return ResponseEntity.ok(new MessageResponse("Fajl je uspješno obrisan."));
    }

    @Operation(
            summary = "Ažuriranje metadata fajla",
            description = "Mijenja opis, tagove, kategoriju ili public status. Ne mijenja sam fajl."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata uspješno ažurirani"),
            @ApiResponse(responseCode = "403", description = "Nemate pravo izmjene ovog fajla",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/files/{id}/metadata")
    public ResponseEntity<FileMetadataResponse> updateFileMetadata(
            @Parameter(description = "ID fajla", example = "42")
            @PathVariable Long id,

            @Valid @RequestBody UpdateFileMetadataRequest request,
            HttpServletRequest httpRequest
    ) {
        FileMetadataResponse response = fileService.updateFileMetadata(id, request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Promjena vidljivosti fajla",
            description = "Brza promjena statusa javne dostupnosti fajla (javno/privatno)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vidljivost uspješno promijenjena"),
            @ApiResponse(responseCode = "403", description = "Nemate pravo izmjene ovog fajla",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/files/{id}/visibility")
    public ResponseEntity<FileMetadataResponse> updateFileVisibility(
            @Parameter(description = "ID fajla", example = "42")
            @PathVariable Long id,
            @Valid @RequestBody UpdateFileVisibilityRequest request,
            HttpServletRequest httpRequest
    ) {
        FileMetadataResponse response = fileService.updateFileVisibility(id, request, httpRequest);
        return ResponseEntity.ok(response);
    }

}
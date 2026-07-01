package com.minio.storage.controller;

import com.minio.storage.dto.*;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.FileVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Verzije fajlova", description = "Upravljanje verzijama fajlova. Aktivno samo ako je versioning uključen na bucketu")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Validated
@SuppressWarnings("unused")
public class FileVersionController {

    private final FileVersionService fileVersionService;

    @Operation(
            summary = "Lista verzija fajla",
            description = "Vraća paginiranu listu svih verzija za određeni fajl. Redoslijed je podrazumijevano od najnovije verzije. " +
                    "Dostupno vlasniku fajla i administratoru."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista verzija uspješno dobavljena"),
            @ApiResponse(responseCode = "401", description = "Korisnik nije autentifikovan",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Nemate dozvolu za pristup verzijama ovog fajla",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{fileId}/versions")
    public ResponseEntity<Page<FileVersionSummaryResponse>> listVersions(
            @Parameter(description = "ID fajla", example = "42")
            @PathVariable Long fileId,
            @ParameterObject
            @PageableDefault(
                    size = 20,
                    sort = "versionNumber",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        Page<FileVersionSummaryResponse> versions =
                fileVersionService.listVersions(fileId, pageable);
        return ResponseEntity.ok(versions);
    }

    @Operation(
            summary = "Detalji specifične verzije fajla",
            description = "Vraća sve metapodatke za jednu verziju koristeći njen interni DB ID. Dostupno vlasniku fajla i administratoru."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalji verzije uspješno dobavljeni"),
            @ApiResponse(responseCode = "403", description = "Samo vlasnik ili admin mogu pristupiti verzijama"),
            @ApiResponse(responseCode = "404", description = "Verzija ili fajl nisu pronađeni")
    })
    @GetMapping("/{fileId}/versions/{versionId}")
    public ResponseEntity<FileVersionDetailResponse> getVersionDetails(
            @Parameter(description = "ID fajla", example = "123") @PathVariable Long fileId,
            @Parameter(description = "Interni DB ID verzije", example = "10") @PathVariable Long versionId) {
        FileVersionDetailResponse response =
                fileVersionService.getVersionDetails(fileId, versionId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Generisanje linka za preuzimanje specifične verzije",
            description = "Kreira privremeni URL za preuzimanje starije verzije fajla. Link važi ograničeno vrijeme."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Link uspješno generisan"),
            @ApiResponse(responseCode = "403", description = "Nemate pravo pristupa verzijama ovog fajla"),
            @ApiResponse(responseCode = "404", description = "Fajl ili verzija nisu pronađeni")
    })
    @GetMapping("/{fileId}/versions/{versionId}/download-link")
    public ResponseEntity<PresignedUrlResponse> generateDownloadLink(
            @PathVariable Long fileId,
            @PathVariable Long versionId,
            @RequestParam(required = false) Integer expiresInSeconds,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(fileVersionService.generateDownloadLink(fileId, versionId, expiresInSeconds, request));
    }

    @Operation(
            summary = "Vraća (Restore) staru verziju fajla",
            description = "Kopira odabranu staru verziju i postavlja je kao trenutno aktivnu (najnoviju) verziju. Stara verzija ostaje netaknuta u istoriji."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verzija uspješno vraćena"),
            @ApiResponse(responseCode = "400", description = "Nema dovoljno prostora u bucketu"),
            @ApiResponse(responseCode = "403", description = "Nemate pravo izmjene ovog fajla"),
            @ApiResponse(responseCode = "404", description = "Fajl ili verzija nisu pronađeni")
    })
    @PostMapping("/{fileId}/versions/{versionId}/restore")
    public ResponseEntity<MessageResponse> restoreVersion(
            @Parameter(description = "ID fajla", example = "42") @PathVariable Long fileId,
            @Parameter(description = "Interni DB ID verzije za restore", example = "10") @PathVariable Long versionId,
            HttpServletRequest request
    ) {
        MessageResponse response = fileVersionService.restoreVersion(fileId, versionId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Brisanje specifične verzije fajla",
            description = "Trajno briše određenu verziju fajla sa skladišta i iz baze. Dozvoljeno samo administratoru."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verzija uspješno obrisana"),
            @ApiResponse(responseCode = "400", description = "Ne može se obrisati jedina preostala verzija",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Nemate privilegije za brisanja verzije",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl ili verzija nije pronađena",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{fileId}/versions/{versionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteVersion(
            @Parameter(description = "ID fajla", example = "42")
            @PathVariable Long fileId,
            @Parameter(description = "ID verzije", example = "15")
            @PathVariable Long versionId,

            HttpServletRequest request
    ) {
        return ResponseEntity.ok(fileVersionService.deleteVersion(fileId, versionId, request));
    }

    @Operation(
            summary = "Upload nove verzije fajla",
            description = "Otprema novu verziju postojećeg fajla. Bucket mora imati uključeno verzioniranje."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Nova verzija uspješno otpremljena"),
            @ApiResponse(responseCode = "400", description = "Verzioniranje nije uključeno ili nema dovoljno prostora",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Nemate pravo izmjene ovog fajla",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/{fileId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadNewVersion(
            @Parameter(description = "ID postojećeg fajla", example = "42")
            @PathVariable Long fileId,
            @Parameter(description = "Nova verzija fajla")
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        FileUploadResponse response = fileVersionService.uploadNewVersion(fileId, file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
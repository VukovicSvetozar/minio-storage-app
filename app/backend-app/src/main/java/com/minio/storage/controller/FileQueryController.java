package com.minio.storage.controller;

import com.minio.storage.dto.FileMetadataResponse;
import com.minio.storage.dto.FileSearchRequest;
import com.minio.storage.enums.FileCategory;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Pretraga Fajlova", description = "Rute za pretragu, filtriranje i dohvatanje metapodataka fajlova")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@SuppressWarnings("unused")
public class FileQueryController {

    private final FileService fileService;

    @Operation(
            summary = "Detalji fajla po ID-u",
            description = "Vraća kompletan metadata zapis o fajlu. Provjerava prava pristupa."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fajl pronađen"),
            @ApiResponse(responseCode = "403", description = "Nemate pristup ovom fajlu",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/{id:\\d+}")
    public ResponseEntity<FileMetadataResponse> getFileById(
            @Parameter(description = "ID fajla", example = "42")
            @PathVariable Long id
    ) {
        FileMetadataResponse file = fileService.getFileById(id);
        return ResponseEntity.ok(file);
    }

    @Operation(
            summary = "Prikaz lične kolekcije fajlova",
            description = "Vraća paginiranu listu svih fajlova koje je trenutno prijavljeni korisnik otpremio. " +
                    "Rezultati su podrazumijevano sortirani po datumu otpremanja (noviji ka starijim)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista fajlova uspješno dobavljena"),
            @ApiResponse(responseCode = "401", description = "Korisnik nije autentifikovan",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/my")
    public ResponseEntity<Page<FileMetadataResponse>> getMyFiles(
            @ParameterObject
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<FileMetadataResponse> files = fileService.getMyFiles(pageable);
        return ResponseEntity.ok(files);
    }

    @Operation(
            summary = "Pregled javno dostupnih fajlova",
            description = "Vraća paginiranu listu svih fajlova koji su globalno dostupni. " +
                    "Uključuje fajlove koji su eksplicitno označeni kao javni, " +
                    "kao i sve privatne fajlove koji se nalaze unutar javno dostupnih bucketa. " +
                    "Ovaj endpoint ne zahtijeva autentifikaciju."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista javnih fajlova uspješno dobavljena")
    })
    @GetMapping("/files/public")
    public ResponseEntity<Page<FileMetadataResponse>> getPublicFiles(
            @ParameterObject
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<FileMetadataResponse> files = fileService.getPublicFiles(pageable);
        return ResponseEntity.ok(files);
    }

    @Operation(
            summary = "Filtriranje fajlova po kategoriji",
            description = "Vraća paginiranu listu fajlova za određenu kategoriju (npr. IMAGE, DOCUMENT)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista fajlova po kategoriji uspješno dobavljena"),
            @ApiResponse(responseCode = "400", description = "Nevalidna kategorija proslijeđena u putanji",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/category/{category}")
    public ResponseEntity<Page<FileMetadataResponse>> getFilesByCategory(
            @Parameter(description = "Kategorija fajlova (IMAGE, DOCUMENT, VIDEO, AUDIO, ARCHIVE, OTHER)",
                    example = "IMAGE")
            @PathVariable FileCategory category,
            @ParameterObject
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<FileMetadataResponse> files = fileService.getFilesByCategory(category, pageable);
        return ResponseEntity.ok(files);
    }

    @Operation(
            summary = "Lista fajlova u skladištu",
            description = "Vraća paginiranu listu metapodataka svih fajlova koji se nalaze u navedenom skladištu."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista fajlova uspješno dobavljena"),
            @ApiResponse(responseCode = "403", description = "Nemate dozvolu za pristup sadržaju ovog skladišta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Skladište sa zadatim nazivom nije pronađeno",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/buckets/{bucketName}/files")
    public ResponseEntity<Page<FileMetadataResponse>> getFilesByBucket(
            @Parameter(description = "Naziv bucketa", example = "moje-slike")
            @PathVariable String bucketName,
            @ParameterObject
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<FileMetadataResponse> files = fileService.getFilesByBucket(bucketName, pageable);
        return ResponseEntity.ok(files);
    }

    @Operation(
            summary = "Napredna pretraga fajlova",
            description = "Omogućava pretragu fajlova prema različitim kriterijumima kao što su naziv, opis, tagovi, kategorija, tip sadržaja, veličina i datum otpremanja. " +
                    "Rezultati su ograničeni na fajlove kojima trenutni korisnik ima pristup."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pretraga uspješno izvršena")
    })
    @PostMapping("/files/search")
    public ResponseEntity<Page<FileMetadataResponse>> searchFiles(
            @Valid @RequestBody FileSearchRequest request,
            @ParameterObject
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<FileMetadataResponse> files = fileService.searchFiles(request, pageable);
        return ResponseEntity.ok(files);
    }

}
package com.minio.storage.controller;

import com.minio.storage.dto.*;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "Statistika fajlova",
        description = "Statistički pregledi skladišta fajlova za korisnike i administratore"
)
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@SuppressWarnings("unused")
public class FileStatisticsController {

    private final FileService fileService;

    @Operation(
            summary = "Lična statistika korisnika",
            description = "Vraća statistiku fajlova prijavljenog korisnika uključujući ukupan broj fajlova, ukupnu veličinu, raspodjelu po kategorijama i raspodjelu po bucketima."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistika uspješno dohvaćena"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Korisnik nije autentifikovan",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/files/statistics/my")
    public ResponseEntity<UserFileStatisticsResponse> getMyStatistics() {
        UserFileStatisticsResponse stats = fileService.getMyStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Globalna statistika fajlova",
            description = "Vraća sveobuhvatnu statistiku svih fajlova u sistemu: " +
                    "ukupan broj, zauzeće prostora, distribucija po kategorijama, korisnicima i bucketima. " +
                    "Zahtijeva ADMIN rolu. Prikazuje isključivo fajlove u aktivnim (neobrisanim) bucketima."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Globalna statistika uspješno dohvaćena"),
            @ApiResponse(responseCode = "401", description = "Korisnik nije autentifikovan",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Korisnik nema ADMIN privilegije",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FileStatisticsResponse> getGlobalStatistics() {
        FileStatisticsResponse stats = fileService.getGlobalStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Statistika zauzeća skladišta po korisnicima",
            description = "Vraća listu korisnika sa brojem fajlova i zauzetim prostorom, " +
                    "sortirano po zauzeću (najveći prvo). Maksimalno 1000 korisnika. " +
                    "Prikazuje isključivo fajlove u aktivnim bucketima."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistika korisnika uspješno dohvaćena"),
            @ApiResponse(responseCode = "401", description = "Korisnik nije autentifikovan",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Korisnik nema ADMIN privilegije",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/statistics/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserStorageStatisticsResponse>> getUserStorageStatistics() {
        List<UserStorageStatisticsResponse> stats = fileService.getUserStorageStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Statistika po kategorijama",
            description = "Detaljne informacije o distribuciji fajlova po kategorijama. " +
                    "Zahtijeva ADMIN rolu. Uključuje samo podatke iz aktivnih bucketa."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistika kategorija uspješno dohvaćena"),
            @ApiResponse(responseCode = "401", description = "Korisnik nije autentifikovan",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Korisnik nema ADMIN privilegije",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/statistics/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryStatisticsResponse>> getCategoryStatistics() {
        List<CategoryStatisticsResponse> stats = fileService.getCategoryStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Statistika zauzeća prostora po bucketima",
            description = "Vraća listu bucketa sa brojem fajlova i zauzetim prostorom, " +
                    "sortirano po zauzeću (najveći prvo). Maksimalno 1000 bucketa. " +
                    "Zahtijeva ADMIN rolu. Prikazuje isključivo aktivne buckete."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistika bucketa uspješno dohvaćena"),
            @ApiResponse(responseCode = "401", description = "Korisnik nije autentifikovan",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Korisnik nema ADMIN privilegije",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files/statistics/buckets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BucketStatisticsResponse>> getBucketStatistics() {
        List<BucketStatisticsResponse> stats = fileService.getBucketStatistics();
        return ResponseEntity.ok(stats);
    }

}
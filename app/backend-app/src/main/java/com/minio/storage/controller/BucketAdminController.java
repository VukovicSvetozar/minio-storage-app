package com.minio.storage.controller;

import com.minio.storage.dto.BucketHealthResponse;
import com.minio.storage.dto.BucketResponse;
import com.minio.storage.dto.BucketSyncResponse;
import com.minio.storage.dto.OrphanCleanupReport;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.BucketService;
import com.minio.storage.service.OrphanObjectCleanupService;
import com.minio.storage.util.ActivityLogHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Bucketi - Administracija",
        description = "Administrativne operacije: sinhronizacija, rekalkulacija i dijagnostika")
@RestController
@RequestMapping("/api/admin/buckets")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
@Slf4j
@SuppressWarnings("unused")
public class BucketAdminController {

    private final BucketService bucketService;
    private final OrphanObjectCleanupService orphanObjectCleanupService;
    private final ActivityLogHelper activityLogHelper;

    @Operation(
            summary = "Sinhronizacija sa MinIO serverom",
            description = "Čita listu bucketa sa storage servera i usklađuje je sa bazom podataka. " +
                    "Bucketi koji postoje samo na storage serveru se uvoze, a postojeći se ažuriraju. " +
                    "Svaki bucket se obrađuje nezavisno - greška na jednom ne prekida ostale."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sinhronizacija završena"),
            @ApiResponse(responseCode = "500", description = "Greška na storage serveru",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/sync")
    public ResponseEntity<BucketSyncResponse> syncBuckets(HttpServletRequest request) {
        BucketSyncResponse response = bucketService.syncBucketsFromMinIO();
        activityLogHelper.logBucketSync(response, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Rekalkulacija statistike bucketa",
            description = "Ponovo izračunava i ispravlja broj fajlova i ukupnu veličinu za navedeni bucket direktno iz baze podataka. " +
                    "Koristi se kada postoji sumnja da su interni brojači fileCount i totalSize postali nekonzistentni."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistika uspješno osvježena - response sadrži ažurirane vrijednosti fileCount i totalSize",
                    content = @Content(schema = @Schema(implementation = BucketResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Bucket nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{bucketName}/recalculate-stats")
    public ResponseEntity<BucketResponse> recalculateStats(
            @Parameter(description = "Naziv bucketa", example = "moje-slike")
            @PathVariable String bucketName,
            HttpServletRequest request
    ) {
        BucketResponse response = bucketService.recalculateBucketStatistics(bucketName);
        activityLogHelper.logBucketStatsRecalculation(bucketName, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Provjera zdravlja bucketa",
            description = "Dijagnostički endpoint. Provjerava da li bucket postoji u bazi, " +
                    "da li postoji na MinIO serveru i da li je sistem za skladištenje dostupan. " +
                    "Korisno za brzu dijagnozu problema."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provjera izvršena (rezultat u tijelu odgovora)"),
            @ApiResponse(responseCode = "500", description = "Neočekivana greška tokom provjere",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{bucketName}/health")
    public ResponseEntity<BucketHealthResponse> checkHealth(
            @Parameter(description = "Naziv bucketa", example = "moje-slike")
            @PathVariable String bucketName
    ) {
        return ResponseEntity.ok(bucketService.checkBucketHealth(bucketName));
    }


    @Operation(
            summary = "Audit i detekcija orphan objekata (Dry-Run)",
            description = "Analizira bucket i upoređuje fajlove na MinIO serveru sa zapisima u bazi. " +
                    "Vraća izvještaj o nepoklapanjima u oba smjera. Ova operacija je 'Dry-Run' i NE briše podatke."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit uspješno završen"),
            @ApiResponse(responseCode = "404", description = "Bucket nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{bucketName}/cleanup-orphans")
    public ResponseEntity<OrphanCleanupReport> auditOrphans(
            @Parameter(description = "Naziv bucketa", example = "moje-slike")
            @PathVariable String bucketName,
            HttpServletRequest request
    ) {
        OrphanCleanupReport report = orphanObjectCleanupService.findOrphans(bucketName);
        activityLogHelper.logOrphanAudit(bucketName, report.getTotalOrphans(), request);
        return ResponseEntity.ok(report);
    }

}
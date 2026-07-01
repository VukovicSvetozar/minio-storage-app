package com.minio.storage.controller;

import com.minio.storage.dto.*;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.BucketLifecycleService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Lifecycle i Retention",
        description = "Upravljanje lifecycle pravilima i retention policy-jem bucketa. " +
                "Lifecycle pravila automatizuju brisanje i premještanje objekata kroz vrijeme. " +
                "Retention policy štiti objekte od brisanja u definisanom periodu i zahtijeva bucket kreiran sa Object Lock podrškom."
)
@RestController
@RequestMapping("/api/admin/buckets/{bucketName}")
@RequiredArgsConstructor
@Validated
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@SuppressWarnings("unused")
public class BucketLifecycleController {

    private final BucketLifecycleService bucketLifecycleService;
    private final ActivityLogHelper activityLogHelper;

    @GetMapping("/lifecycle")
    @Operation(
            summary = "Dohvati lifecycle konfiguraciju",
            description = "Vraća sva lifecycle pravila definisana na bucketu. " +
                    "Ako bucket nema lifecycle pravila, vraća praznu listu."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Konfiguracija uspješno dohvaćena"),
            @ApiResponse(responseCode = "404", description = "Bucket nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LifecycleConfigResponse> getLifecycleConfig(
            @Parameter(description = "Naziv bucketa", example = "dokumenti")
            @PathVariable String bucketName) {

        return ResponseEntity.ok(bucketLifecycleService.getLifecycleConfig(bucketName));
    }

    @PutMapping("/lifecycle")
    @Operation(
            summary = "Postavi lifecycle pravila",
            description = "Postavlja ili zamjenjuje lifecycle konfiguraciju bucketa. " +
                    "Svako pravilo mora imati expiration (automatsko brisanje) " +
                    "ili transition (premještanje u drugi bucket). " +
                    "Pozivanje ovog endpointa zamjenjuje sva postojeća pravila."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lifecycle pravila uspješno postavljena"),
            @ApiResponse(responseCode = "400", description = "Neispravna pravila u zahtjevu",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bucket nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Greška pri komunikaciji sa MinIO serverom",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LifecycleConfigResponse> setLifecycleConfig(
            @Parameter(description = "Naziv bucketa", example = "dokumenti") @PathVariable String bucketName,
            @Valid @RequestBody LifecycleConfigRequest request,
            HttpServletRequest httpRequest) {

        LifecycleConfigResponse response = bucketLifecycleService.setLifecycleConfig(bucketName, request);

        try {
            activityLogHelper.logBucketUpdate(
                    bucketName,
                    null,
                    "Lifecycle konfiguracija ažurirana - postavljeno " + response.getRuleCount() + " pravila",
                    httpRequest
            );
        } catch (Exception e) {
            log.warn("Nije moguće zapisati audit log za bucket {}: {}", bucketName, e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/lifecycle")
    @Operation(
            summary = "Obriši lifecycle konfiguraciju",
            description = "Uklanja sva lifecycle pravila sa bucketa. " +
                    "Nakon brisanja, objekti se više neće automatski brisati ni premještati. " +
                    "Operacija ne utiče na postojeće objekte."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lifecycle konfiguracija uspješno obrisana"),
            @ApiResponse(responseCode = "404", description = "Bucket nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Greška pri komunikaciji sa MinIO serverom",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> deleteLifecycleConfig(
            @Parameter(description = "Naziv bucketa", example = "dokumenti") @PathVariable String bucketName,
            HttpServletRequest httpRequest) {

        bucketLifecycleService.deleteLifecycleConfig(bucketName);

        try {
            activityLogHelper.logBucketUpdate(
                    bucketName,
                    null,
                    "Uklonjena kompletna lifecycle konfiguracija (automatizacija brisanja/arhiviranja isključena).",
                    httpRequest
            );
        } catch (Exception e) {
            log.warn("Nije moguće zapisati audit log za brisanje lifecycle-a na bucketu {}: {}", bucketName, e.getMessage());
        }

        return ResponseEntity.ok(new MessageResponse("Lifecycle konfiguracija je uspješno uklonjena."));
    }

    @GetMapping("/retention")
    @Operation(
            summary = "Dohvati retention policy",
            description = "Vraća retention konfiguraciju bucketa. " +
                    "Retention policy funkcioniše samo na bucketima kreiranim sa Object Lock podrškom (object-lock=true). " +
                    "Ako Object Lock nije uključen, vraća objectLockEnabled=false umjesto greške."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retention policy uspješno dohvaćen"),
            @ApiResponse(responseCode = "404", description = "Bucket nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RetentionPolicyResponse> getRetentionPolicy(
            @Parameter(description = "Naziv bucketa", example = "dokumenti") @PathVariable String bucketName) {
        return ResponseEntity.ok(bucketLifecycleService.getRetentionPolicy(bucketName));
    }

    @PutMapping("/retention")
    @Operation(
            summary = "Postavi retention policy",
            description = "Postavlja pravila zadržavanja (Retention Policy) na bucket. " +
                    "VAŽNO: Radi samo na bucketima kreiranim sa Object Lock podrškom. " +
                    "GOVERNANCE režim dozvoljava administratorima zaobilaženje zaštite. " +
                    "COMPLIANCE režim strogo štiti objekte do isteka perioda bez mogućnosti poništavanja."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retention policy uspješno postavljen"),
            @ApiResponse(responseCode = "400", description = "Neispravan režim ili trajanje",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bucket nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Konflikt - Bucket nije kreiran sa Object Lock podrškom",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RetentionPolicyResponse> setRetentionPolicy(
            @Parameter(description = "Naziv bucketa", example = "dokumenti") @PathVariable String bucketName,
            @Valid @RequestBody RetentionPolicyRequest request,
            HttpServletRequest httpRequest) {

        RetentionPolicyResponse response = bucketLifecycleService.setRetentionPolicy(bucketName, request);

        try {
            activityLogHelper.logBucketUpdate(
                    bucketName,
                    null,
                    "Postavljena polisa zadržavanja (Retention) - režim: " + request.getMode() +
                            ", trajanje: " + request.getDays() + " dana",
                    httpRequest
            );
        } catch (Exception e) {
            log.warn("Nije moguće zapisati audit log za retention na bucketu {}: {}", bucketName, e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

}
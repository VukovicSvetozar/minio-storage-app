package com.minio.storage.controller;

import com.minio.storage.dto.*;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.BucketService;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Upravljanje Bucketima", description = "Rute za kreiranje, sinhronizaciju i pregled kontejnera (bucketa)")
@RestController
@RequestMapping("/api/buckets")
@RequiredArgsConstructor
@Validated
@SuppressWarnings("unused")
public class BucketController {

    private final BucketService bucketService;
    private final ActivityLogHelper activityLogHelper;

    @Operation(summary = "Provjera dostupnosti naziva bucketa", description = "Provjerava da li je naziv slobodan u bazi i na MinIO serveru")
    @GetMapping("/check-name")
    public ResponseEntity<CheckAvailabilityResponse> checkNameAvailability(
            @Parameter(description = "Naziv bucketa za provjeru", example = "moje-slike")
            @RequestParam
            @NotBlank(message = "Naziv bucketa je obavezan")
            @Size(min = 3, max = 63, message = "Naziv mora imati između 3 i 63 karaktera")
            @Pattern(
                    regexp = "^[a-z0-9](?!.*\\.\\.)[a-z0-9.-]{1,61}[a-z0-9]$",
                    message = "Naziv može sadržavati mala slova, brojeve, tačke i crtice. Ne može početi ili završiti crticom ili tačkom, niti sadržavati dvije uzastopne tačke."
            )
            String name) {

        boolean available = bucketService.isNameAvailable(name);
        return ResponseEntity.ok(new CheckAvailabilityResponse(available, name));
    }

    @Operation(
            summary = "Kreiranje novog bucketa",
            description = "Autentifikovani korisnik kreira novi bucket na MinIO serveru i zapis u bazi podataka."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bucket uspješno kreiran"),
            @ApiResponse(responseCode = "400", description = "Podaci nisu validni",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Naziv bucketa je već zauzet",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<BucketResponse> createBucket(
            @Valid @RequestBody CreateBucketRequest request,
            HttpServletRequest httpRequest) {

        BucketResponse response = bucketService.createBucket(request);

        activityLogHelper.logBucketCreate(
                response.getName(),
                response.getId(),
                httpRequest
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Brisanje bucketa", description = "Vlasnik ili admin briše bucket. Izvršava se fizičko brisanje na MinIO i soft-delete u bazi.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Buket uspješno obrisan"),
            @ApiResponse(responseCode = "403", description = "Nemate prava za brisanje ovog bucketa"),
            @ApiResponse(responseCode = "404", description = "Buket nije pronađen"),
            @ApiResponse(responseCode = "409", description = "Konflikt: Bucket nije prazan")
    })
    @DeleteMapping("/name/{name}")
    public ResponseEntity<MessageResponse> deleteBucket(
            @Parameter(description = "Jedinstveni naziv bucketa", example = "moje-slike")
            @Pattern(
                    regexp = "^[a-z0-9](?!.*\\.\\.)[a-z0-9.-]{1,61}[a-z0-9]$",
                    message = "Neispravan naziv bucketa"
            )
            @PathVariable String name,
            HttpServletRequest httpRequest) {

        bucketService.deleteBucket(name);
        activityLogHelper.logBucketDelete(name, httpRequest);

        return ResponseEntity.ok(new MessageResponse("Buket '" + name + "' je uspješno obrisan."));
    }

    @Operation(summary = "Promjena polise privatnosti", description = "Vlasnik ili admin mijenja status bucketa iz Public u Private i obrnuto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Polisa uspješno ažurirana"),
            @ApiResponse(responseCode = "403", description = "Nemate prava za izmjenu ovog bucketa"),
            @ApiResponse(responseCode = "404", description = "Buket nije pronađen")
    })
    @PutMapping("/name/{name}/policy")
    public ResponseEntity<BucketResponse> updatePolicy(
            @Parameter(description = "Jedinstveni naziv bucketa", example = "moje-slike")
            @Pattern(
                    regexp = "^[a-z0-9](?!.*\\.\\.)[a-z0-9.-]{1,61}[a-z0-9]$",
                    message = "Neispravan naziv bucketa"
            )
            @PathVariable String name,
            @Valid @RequestBody UpdateBucketPolicyRequest request,
            HttpServletRequest httpRequest) {

        BucketResponse response = bucketService.updateBucketPolicy(name, request);

        activityLogHelper.logBucketUpdate(
                response.getName(),
                response.getId(),
                httpRequest
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Detalji buketa po ID-u", description = "Vraća detaljne informacije o specifičnom buketu")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Buket pronađen"),
            @ApiResponse(responseCode = "404", description = "Buket nije pronađen ili je obrisan")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BucketResponse> getBucketById(@PathVariable Long id) {
        return ResponseEntity.ok(bucketService.getBucketById(id));
    }

    @Operation(
            summary = "Detalji buketa po nazivu",
            description = "Vraća detaljne informacije o buketu koristeći njegov jedinstveni naziv"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Buket pronađen"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Buket sa tim nazivom nije pronađen ili je obrisan",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/name/{name}")
    public ResponseEntity<BucketResponse> getBucketByName(
            @Parameter(
                    description = "Jedinstveni naziv buketa (lowercase, MinIO kompatibilno)",
                    example = "moje-slike"
            )
            @Pattern(
                    regexp = "^[a-z0-9](?!.*\\.\\.)[a-z0-9.-]{1,61}[a-z0-9]$",
                    message = "Neispravan naziv bucketa"
            )
            @PathVariable String name) {

        return ResponseEntity.ok(bucketService.getBucketByName(name));
    }

    @Operation(
            summary = "Lista svih aktivnih bucketa",
            description = "Vraća listu svih bucketa koji nisu obrisani (soft-deleted). Pristup je dozvoljen samo administratorima."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BucketResponse>> getAllBuckets() {
        return ResponseEntity.ok(bucketService.getAllBuckets());
    }

    @Operation(
            summary = "Moji bucketi",
            description = "Lista bucketa čiji je vlasnik trenutno prijavljeni korisnik"
    )
    @GetMapping("/my")
    public ResponseEntity<List<BucketResponse>> getMyBuckets() {
        return ResponseEntity.ok(bucketService.getMyBuckets());
    }

    @Operation(
            summary = "Javni bucketi",
            description = "Lista svih bucketa koji su označeni kao javni"
    )
    @GetMapping("/public")
    public ResponseEntity<List<BucketResponse>> getPublicBuckets() {
        return ResponseEntity.ok(bucketService.getPublicBuckets());
    }

    @Operation(
            summary = "Dohvati status verzioniranja",
            description = "Provjerava da li je verzioniranje uključeno za bucket."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status uspješno vraćen"),
            @ApiResponse(responseCode = "403", description = "Nemate pravo pristupa informacijama o ovom bucketu",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Bucket nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{name}/versioning")
    public ResponseEntity<VersioningStatusResponse> getVersioningStatus(@PathVariable String name) {
        return ResponseEntity.ok(bucketService.getVersioningStatus(name));
    }

    @Operation(
            summary = "Ažuriraj verzioniranje bucketa",
            description = "Omogućava uključivanje (Enabled) ili pauziranje (Suspended) verzioniranja. Samo ADMIN ima pristup."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Versioning uspješno ažuriran"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Nemate privilegije",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Bucket nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{name}/versioning")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BucketResponse> updateVersioning(
            @PathVariable String name,
            @Valid @RequestBody VersioningUpdateBucketRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(bucketService.updateBucketVersioning(name, request, httpRequest));
    }

}
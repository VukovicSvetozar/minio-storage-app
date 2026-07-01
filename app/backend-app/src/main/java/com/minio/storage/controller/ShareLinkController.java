package com.minio.storage.controller;

import com.minio.storage.dto.CreateShareLinkRequest;
import com.minio.storage.dto.MessageResponse;
import com.minio.storage.dto.ShareLinkAccessResponse;
import com.minio.storage.dto.ShareLinkResponse;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.ShareLinkService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Share linkovi", description = "Upravljanje share linkovima za dijeljenje fajlova bez autentifikacije")
@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
@Validated
@SuppressWarnings("unused")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;
    private final ActivityLogHelper activityLogHelper;

    @PostMapping("/create/{fileId}")
    @Operation(
            summary = "Kreiranje share linka",
            description = "Generiše jedinstveni token kojim anonimni korisnik može pristupiti fajlu bez prijave. " +
                    "Pravo na kreiranje imaju vlasnik fajla i administrator."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Share link uspješno kreiran"),
            @ApiResponse(responseCode = "400", description = "Neispravni podaci u zahtjevu",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Nemate dozvolu za dijeljenje ovog fajla",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fajl nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ShareLinkResponse> createShareLink(
            @Parameter(description = "ID fajla koji se dijeli") @PathVariable Long fileId,
            @Valid @RequestBody CreateShareLinkRequest request,
            HttpServletRequest httpRequest) {
        ShareLinkResponse response = shareLinkService.createShareLink(fileId, request);
        activityLogHelper.logShareCreate(response.getOriginalFileName(), response.getId(), httpRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/public/{shareToken}")
    @Operation(
            summary = "Pristup fajlu putem share linka",
            description = "Javni endpoint - ne zahtijeva autentifikaciju. Vraća presigned URL za preuzimanje fajla."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pristup odobren"),
            @ApiResponse(responseCode = "404", description = "Share link nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "Share link je istekao ili deaktiviran",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ShareLinkAccessResponse> accessShareLink(
            @Parameter(description = "Jedinstveni token share linka") @PathVariable String shareToken,
            HttpServletRequest httpRequest) {
        ShareLinkAccessResponse response = shareLinkService.accessShareLink(shareToken);
        activityLogHelper.logShareAccess(response.getOriginalFileName(), httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-shares")
    @Operation(
            summary = "Lista mojih share linkova",
            description = "Vraća sve share linkove trenutnog korisnika. " +
                    "Parametar activeOnly=true filtrira samo aktivne i neistekle."
    )
    @ApiResponse(responseCode = "200", description = "Lista share linkova uspješno dohvaćena")
    public ResponseEntity<List<ShareLinkResponse>> getMyShareLinks(
            @Parameter(description = "Prikaži samo aktivne i neistekle linkove", example = "true")
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<ShareLinkResponse> shares = activeOnly
                ? shareLinkService.getMyActiveShareLinks()
                : shareLinkService.getMyShareLinks();
        return ResponseEntity.ok(shares);
    }

    @GetMapping("/{shareId}/stats")
    @Operation(
            summary = "Statistike share linka",
            description = "Vraća detaljne informacije i broj pristupa za određeni share link. " +
                    "Pristup imaju samo vlasnik linka i administrator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistike uspješno dohvaćene"),
            @ApiResponse(responseCode = "403", description = "Nemate dozvolu za pregled ovog share linka",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Share link nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ShareLinkResponse> getShareLinkStats(
            @Parameter(description = "Interni ID share linka", required = true, example = "101") @PathVariable Long shareId) {
        return ResponseEntity.ok(shareLinkService.getShareLinkStats(shareId));
    }

    @PutMapping("/{shareId}/deactivate")
    @Operation(
            summary = "Deaktivacija share linka",
            description = "Ručno onemogućava pristup fajlu putem ovog linka. Rekord ostaje u bazi radi statistike, ali link postaje nevažeći."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Share link uspješno deaktiviran"),
            @ApiResponse(responseCode = "403", description = "Nemate dozvolu za deaktivaciju ovog linka",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Share link nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> deactivateShareLink(
            @Parameter(description = "ID share linka", example = "101")
            @PathVariable Long shareId) {

        shareLinkService.deactivateShareLink(shareId);
        return ResponseEntity.ok(
                new MessageResponse("Share link je uspješno deaktiviran.")
        );
    }

    @DeleteMapping("/{shareId}")
    @Operation(
            summary = "Brisanje share linka",
            description = "Trajno briše rekord o share linku iz baze podataka. Nakon ove operacije, pristup fajlu putem tog tokena više nije moguć."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Share link uspješno obrisan"),
            @ApiResponse(responseCode = "403", description = "Nemate dozvolu za brisanje ovog linka",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Share link nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> deleteShareLink(
            @Parameter(description = "ID share linka", example = "101") @PathVariable Long shareId,
            HttpServletRequest httpRequest) {

        shareLinkService.deleteShareLink(shareId);
        activityLogHelper.logShareDelete(shareId, httpRequest);

        return ResponseEntity.ok(new MessageResponse("Share link je uspješno obrisan."));
    }

}
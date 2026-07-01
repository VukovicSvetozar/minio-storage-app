package com.minio.storage.controller;

import com.minio.storage.dto.MessageResponse;
import com.minio.storage.dto.UpdateUserRoleRequest;
import com.minio.storage.dto.UserResponse;
import com.minio.storage.dto.UserUpdateResponse;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.UserService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Upravljanje korisnicima", description = "Administrativne operacije nad korisničkim nalozima")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@SuppressWarnings("unused")
public class UserController {

    private final UserService userService;
    private final ActivityLogHelper activityLogHelper;

    @GetMapping
    @Operation(
            summary = "Lista svih korisnika",
            description = "Vraća listu svih registrovanih korisnika sa osnovnim podacima. Pristup dozvoljen isključivo administratorima."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista korisnika uspješno dohvaćena"),
            @ApiResponse(responseCode = "403", description = "Nemate privilegije za pregled liste korisnika",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Detalji korisnika po ID-u",
            description = "Vraća podatke o pojedinačnom korisniku. Pristup dozvoljen isključivo administratorima."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Korisnik uspješno pronađen"),
            @ApiResponse(responseCode = "403", description = "Nemate dozvolu za pristup podacima o korisnicima",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Korisnik sa navedenim ID-em ne postoji u bazi",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID korisnika", example = "1", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}/role")
    @Operation(
            summary = "Promjena uloge korisnika",
            description = "Mijenja ulogu korisnika između USER i ADMIN. Dostupno samo administratorima."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uloga je uspješno ažurirana"),
            @ApiResponse(responseCode = "400", description = "Zahtjev nije validan ili je uloga neispravna",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserUpdateResponse> updateUserRole(
            @Parameter(description = "ID korisnika", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            HttpServletRequest httpRequest) {

        UserResponse updatedUser = userService.updateUserRole(id, request.getRole());

        activityLogHelper.logUserUpdate(
                updatedUser.getUsername(),
                updatedUser.getId(),
                httpRequest
        );

        return ResponseEntity.ok(
                UserUpdateResponse.builder()
                        .message("Uloga korisnika je uspješno promijenjena.")
                        .user(updatedUser)
                        .build()
        );
    }

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Promjena statusa korisnika",
            description = "Uključuje ili isključuje korisnički nalog (aktivacija/deaktivacija). Dostupno isključivo administratorima."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status uspješno promijenjen",
                    content = @Content(schema = @Schema(implementation = UserUpdateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Pokušaj izmjene sopstvenog statusa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Pokušaj izmjene sistemskog naloga",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserUpdateResponse> toggleUserStatus(
            @Parameter(description = "ID korisnika", example = "1") @PathVariable Long id,
            HttpServletRequest httpRequest) {

        UserResponse updatedUser = userService.toggleUserStatus(id);

        activityLogHelper.logUserUpdate(
                updatedUser.getUsername(),
                updatedUser.getId(),
                httpRequest
        );

        String statusMessage = updatedUser.getIsActive() ?
                "Korisnički nalog je uspješno aktiviran." :
                "Korisnički nalog je uspješno deaktiviran.";

        return ResponseEntity.ok(
                UserUpdateResponse.builder()
                        .message(statusMessage)
                        .user(updatedUser)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deaktivacija korisničkog naloga",
            description = "Vrši soft delete korisnika i deaktivira sve njegove share linkove. " +
                    "Fajlovi i bucketi ostaju u sistemu radi revizije. " +
                    "Nije moguće obrisati sistemski niti sopstveni nalog."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Korisnik uspješno deaktiviran"),
            @ApiResponse(responseCode = "400", description = "Pokušaj brisanja sopstvenog naloga",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Pokušaj brisanja sistemskog naloga",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> deleteUser(
            @Parameter(description = "ID korisnika", example = "1") @PathVariable Long id,
            HttpServletRequest httpRequest) {

        UserResponse deactivatedUser = userService.deactivateUser(id);
        activityLogHelper.logUserDelete(
                deactivatedUser.getUsername(),
                id,
                httpRequest
        );
        return ResponseEntity.ok(
                new MessageResponse("Korisnički nalog je uspješno deaktiviran."));
    }

}
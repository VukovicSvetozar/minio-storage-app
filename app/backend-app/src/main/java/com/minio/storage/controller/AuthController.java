package com.minio.storage.controller;

import com.minio.storage.dto.*;
import com.minio.storage.exception.AppException;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "Autentifikacija",
        description = "Upravljanje korisničkim nalozima i sesijama (registracija, verifikacija i autentifikacija) bazirano na JWT standardu.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SuppressWarnings("unused")
@Validated
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registracija novog korisnika", description = "Kreira novi korisnički nalog i šalje verifikacioni email sa jedinstvenim ključem")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Korisnik uspješno kreiran. Tokeni nisu izdati jer je potrebna verifikacija emaila."),
            @ApiResponse(responseCode = "400", description = "Korisničko ime ili email su već u upotrebi",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Verifikacija email adrese",
            description = "Aktivira korisnički nalog pomoću verifikacionog ključa poslatog na email adresu. " +
                    "Ako je nalog već verifikovan, biće vraćeni važeći tokeni.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email uspješno verifikovan ili je nalog već bio aktivan"),
            @ApiResponse(responseCode = "400", description = "Nevažeći ili istekao ključ",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody VerificationRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.verifyEmail(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Ponovno slanje verifikacionog emaila",
            description = "Generiše novi ključ i šalje ga na email korisnika. " +
                    "Zahtjev je ograničen vremenskim intervalom kako bi se spriječila zloupotreba (maksimalno 1 zahtjev u 2 minuta)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Zahtjev obrađen (email poslat ako nalog postoji)"),
            @ApiResponse(responseCode = "400", description = "Neispravno korisničko ime, nalog je već verifikovan ili je zatražen prebrzo (Rate Limit)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Korisnik nije pronađen",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Greška pri slanju emaila",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerificationEmail(
            @Parameter(description = "Korisničko ime za koje se ponovo šalje verifikacija", example = "vuk_pro")
            @RequestParam
            @NotBlank(message = "Korisničko ime je obavezno")
            @Size(min = 3, max = 30, message = "Korisničko ime mora imati između 3 i 30 karaktera")
            @Pattern(regexp = "^[a-z0-9_]*$", message = "Korisničko ime može sadržavati samo mala slova, brojeve i donje crte")
            String username,
            HttpServletRequest httpRequest) {
        authService.resendVerificationEmail(username, httpRequest);
        return ResponseEntity.ok(new MessageResponse("Verifikacioni email je uspješno poslat"));
    }

    @Operation(
            summary = "Provjera dostupnosti korisničkog imena",
            description = "Provjerava da li je korisničko ime slobodno za registraciju"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provjera uspješna"),
            @ApiResponse(responseCode = "400", description = "Neispravan format korisničkog imena",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/check-username/{username}")
    public ResponseEntity<CheckAvailabilityResponse> checkUsernameAvailability(
            @Parameter(description = "Korisničko ime (3-30 karaktera, mala slova, brojevi i _)", example = "vuk_123")
            @PathVariable
            @NotBlank(message = "Korisničko ime je obavezno")
            @Size(min = 3, max = 30, message = "Korisničko ime mora imati između 3 i 30 karaktera")
            @Pattern(regexp = "^[a-z0-9_]*$", message = "Korisničko ime može sadržavati samo mala slova, brojeve i donje crte")
            String username) {

        boolean available = !authService.isUsernameTaken(username);
        return ResponseEntity.ok(new CheckAvailabilityResponse(available, username));
    }

    @Operation(
            summary = "Provjera dostupnosti email adrese",
            description = "Provjerava da li je email adresa slobodna za registraciju"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provjera uspješna"),
            @ApiResponse(responseCode = "400", description = "Neispravan format email adrese",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/check-email")
    public ResponseEntity<CheckAvailabilityResponse> checkEmailAvailability(
            @Parameter(description = "Email adresa korisnika", example = "test@gmail.com")
            @RequestParam
            @NotBlank(message = "Email adresa je obavezna")
            @Email(message = "Format email adrese nije ispravan")
            @Size(max = 100, message = "Email adresa ne može biti duža od 100 karaktera")
            String email) {
        boolean available = !authService.isEmailTaken(email);
        return ResponseEntity.ok(new CheckAvailabilityResponse(available, email));
    }

    @Operation(
            summary = "Prijava na sistem",
            description = "Autentifikuje korisnika putem korisničkog imena ili email adrese i lozinke. " +
                    "Vraća JWT access token i refresh token. Email mora biti verifikovan."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Uspješna prijava - vraćeni su access i refresh tokeni"),
            @ApiResponse(responseCode = "401",
                    description = "Pogrešno korisničko ime ili lozinka",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "Nalog nije verifikovan ili je suspendovan",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trenutni korisnik", description = "Vraća osnovne podatke o trenutno prijavljenom korisniku na osnovu tokena")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Uspješno"),
            @ApiResponse(responseCode = "401", description = "Niste autorizovani",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Osvježavanje tokena", description = "Vraća novi Access Token koristeći važeći Refresh Token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token uspješno osvježen"),
            @ApiResponse(responseCode = "401", description = "Nevažeći ili istekao refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Odjava korisnika",
            description = "Poništava trenutni pristupni token i sve refresh tokene korisnika u bazi. Korisnik će biti odjavljen sa svih uređaja."
    )
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request,
            Principal principal) {
        String accessToken = authHeader.substring(7);
        String username = principal.getName();
        authService.logout(accessToken);
        return ResponseEntity.ok(new MessageResponse("Uspješno ste se odjavili"));
    }

    @Operation(summary = "Zahtjev za oporavak lozinke", description = "Šalje email sa linkom za postavljanje nove lozinke")
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        authService.forgotPassword(request, httpRequest);

        return ResponseEntity.ok(new MessageResponse("Ukoliko nalog postoji, uputstvo za oporavak lozinke je poslato na vaš email"));
    }

    @Operation(summary = "Promjena lozinke", description = "Postavlja novu lozinku koristeći dobijeni token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lozinka uspješno promijenjena"),
            @ApiResponse(responseCode = "400", description = "Nevažeći ili istekao token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        authService.resetPassword(request, httpRequest);

        return ResponseEntity.ok(new MessageResponse("Lozinka je uspješno promijenjena. Sada se možete prijaviti."));
    }

    @Operation(summary = "Validacija tokena za reset", description = "Provjerava da li je token za zaboravljenu lozinku još uvijek važeći")
    @GetMapping("/validate-reset-token")
    public ResponseEntity<MessageResponse> validateResetToken(
            @RequestParam @NotBlank(message = "Token je obavezan") String token) {

        boolean isValid = authService.validateResetToken(token);
        if (!isValid) {
            throw AppException.badRequest("Token je nevažeći ili je istekao");
        }

        return ResponseEntity.ok(new MessageResponse("Token je ispravan"));
    }

}
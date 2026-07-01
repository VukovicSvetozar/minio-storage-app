package com.minio.storage.controller;

import com.minio.storage.dto.SystemHealthResponse;
import com.minio.storage.dto.SystemMetricsResponse;
import com.minio.storage.enums.HealthStatus;
import com.minio.storage.exception.ErrorResponse;
import com.minio.storage.service.HealthMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Zdravlje sistema", description = "Monitoring i dijagnostika sistema")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class HealthController {

    private final HealthMonitoringService healthMonitoringService;

    @Operation(
            summary = "Provjera dostupnosti",
            description = "Jednostavan ping za provjeru da li je aplikacija pokrenuta. " +
                    "Ne zahtijeva autentifikaciju. Koristan za load balancere i monitoring alate."
    )
    @ApiResponse(responseCode = "200", description = "Aplikacija je pokrenuta")
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @Operation(
            summary = "Status sistema",
            description = "Provjerava dostupnost baze podataka i MinIO servera. " +
                    "Vraća UP, DEGRADED ili DOWN sa detaljima po komponenti. " +
                    "Javni endpoint — koristan za load balancere i monitoring alate."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sistem je ispravan (UP)"),
            @ApiResponse(responseCode = "503",
                    description = "Sistem nije dostupan ili je degradiran (DOWN ili DEGRADED)",
                    content = @Content(schema = @Schema(implementation = SystemHealthResponse.class)))
    })
    @GetMapping("/status")
    public ResponseEntity<SystemHealthResponse> getSystemHealth() {
        SystemHealthResponse health = healthMonitoringService.getSystemHealth();
        HttpStatus status = health.getStatus() == HealthStatus.UP
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;
        return new ResponseEntity<>(health, status);
    }

    @Operation(
            summary = "Sistemske metrike",
            description = "Detaljne metrike aplikacije, baze i MinIO storage sistema. " +
                    "Dostupno samo administratorima."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metrike uspješno prikupljene"),
            @ApiResponse(responseCode = "403", description = "Pristup dozvoljen samo administratorima",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemMetricsResponse> getSystemMetrics() {
        return ResponseEntity.ok(healthMonitoringService.getSystemMetrics());
    }

}
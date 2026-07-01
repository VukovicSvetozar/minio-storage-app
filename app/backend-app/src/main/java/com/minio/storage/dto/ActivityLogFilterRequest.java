package com.minio.storage.dto;

import com.minio.storage.enums.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zahtjev za filtriranje dnevnika aktivnosti korisnika")
public class ActivityLogFilterRequest {

    @Schema(description = "Korisničko ime za koje se traže aktivnosti", example = "vuk_admin")
    private String username;

    @Schema(description = "Tip aktivnosti (npr. LOGIN, FILE_UPLOAD, FILE_DELETE)")
    private ActivityType activityType;

    @Schema(description = "Tip resursa nad kojim je izvršena akcija", example = "FileMetadata")
    private String resourceType;

    @PastOrPresent(message = "Početni datum ne može biti u budućnosti")
    @Schema(description = "Početni datum i vrijeme za vremenski opseg pretrage", example = "2024-01-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "Krajnji datum i vrijeme za vremenski opseg pretrage", example = "2024-12-31T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "Filtriranje prema uspješnosti operacije", example = "true")
    private Boolean success;

    @Min(value = 0, message = "Broj stranice ne može biti negativan")
    @Schema(description = "Broj stranice (počinje od 0)", defaultValue = "0", example = "0")
    private Integer page = 0;

    @Min(value = 1, message = "Broj zapisa po stranici mora biti najmanje 1")
    @Max(value = 100, message = "Broj zapisa po stranici ne može preći 100")
    @Schema(description = "Broj zapisa po stranici", defaultValue = "20", example = "20")
    private Integer size = 20;

    @Schema(description = "Polje po kojem se vrši sortiranje", defaultValue = "timestamp", example = "timestamp")
    private String sortBy = "timestamp";

    @Pattern(regexp = "^(?i)(ASC|DESC)$", message = "Smijer sortiranja mora biti ASC ili DESC")
    @Schema(description = "Smijer sortiranja (ASC ili DESC)", defaultValue = "DESC", example = "DESC")
    private String sortDirection = "DESC";

}
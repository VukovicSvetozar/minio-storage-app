package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Zahtjev za postavljanje lifecycle pravila na bucket")
public class LifecycleConfigRequest {

    @NotEmpty(message = "Lista pravila ne smije biti prazna.")
    @Valid
    @Schema(description = "Lista lifecycle pravila")
    private List<LifecycleRule> rules;

    @Data
    @Schema(description = "Pojedinačno lifecycle pravilo")
    public static class LifecycleRule {

        @NotBlank(message = "ID pravila je obavezan.")
        @Schema(description = "Jedinstveni identifikator pravila", example = "archive-old-files")
        private String id;

        @Schema(description = "Da li je pravilo aktivno", example = "true")
        private boolean enabled = true;

        @Schema(description = "Filter - na koje objekte se primjenjuje pravilo (null = svi objekti)")
        private RuleFilter filter;

        @Schema(description = "Pravilo isteka - automatsko brisanje nakon N dana")
        private ExpirationRule expiration;

        @Schema(description = "Pravilo tranzicije - premještanje u drugi bucket nakon N dana")
        private TransitionRule transition;
    }

    @Data
    @Schema(description = "Filter koji određuje na koje objekte se pravilo primjenjuje")
    public static class RuleFilter {
        @Schema(description = "Prefiks putanje objekta", example = "reports/")
        private String prefix;
    }

    @Data
    @Schema(description = "Pravilo automatskog brisanja objekata")
    public static class ExpirationRule {

        @NotNull(message = "Broj dana je obavezan.")
        @Min(value = 1, message = "Broj dana mora biti najmanje 1.")
        @Schema(description = "Broj dana nakon kojih se objekat automatski briše", example = "365")
        private Integer days;

        @Schema(description = "Da li se brišu i istekle verzije objekta", example = "false")
        private boolean expiredObjectDeleteMarker = false;
    }

    @Data
    @Schema(description = "Pravilo premještanja objekata u drugi bucket")
    public static class TransitionRule {

        @NotNull(message = "Broj dana je obavezan za tranziciju.")
        @Min(value = 1, message = "Broj dana mora biti najmanje 1.")
        @Schema(description = "Broj dana nakon kojih se objekat premješta", example = "90")
        private Integer days;

        @NotBlank(message = "Naziv ciljnog bucketa je obavezan za tranziciju.")
        @Schema(description = "Naziv bucketa u koji se objekat premješta", example = "archive-bucket")
        private String targetBucket;
    }

}
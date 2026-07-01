package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detaljna lična statistika fajlova za prijavljenog korisnika")
public class UserFileStatisticsResponse {

    @Schema(description = "Korisničko ime vlasnika fajlova", example = "vuk_pro")
    private String username;

    @Schema(description = "Ukupan broj fajlova koje posjeduje korisnik", example = "45")
    private long totalFiles;

    @Schema(description = "Ukupna veličina svih korisnikovih fajlova u bajtovima", example = "104857600")
    private long totalSizeBytes;

    @Schema(description = "Formatirana ukupna veličina fajlova korisnika", example = "100.0 MB")
    private String totalSizeFormatted;

    @Schema(description = "Broj korisnikovih fajlova grupisan po kategorijama",
            example = "{\"IMAGE\": 20, \"DOCUMENT\": 25}")
    private Map<String, Long> byCategory;

    @Schema(description = "Broj korisnikovih fajlova grupisan po bucketima",
            example = "{\"slike-odmor\": 15, \"posao-privatno\": 30}")
    private Map<String, Long> byBucket;

}
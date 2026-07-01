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
@Schema(description = "Zbirni statistički podaci o svim fajlovima u sistemu (Globalna statistika)")
public class FileStatisticsResponse {

    @Schema(description = "Ukupan broj fajlova uskladištenih u sistemu", example = "1250")
    private long totalFiles;

    @Schema(description = "Ukupna veličina svih fajlova u bajtovima", example = "5368709120")
    private long totalSizeBytes;

    @Schema(description = "Formatirana ukupna veličina fajlova", example = "5.0 GB")
    private String totalSizeFormatted;

    @Schema(description = "Broj fajlova grupisan po kategorijama",
            example = "{\"IMAGE\": 150, \"DOCUMENT\": 300, \"AUDIO\": 50}")
    private Map<String, Long> byCategory;

    @Schema(description = "Zauzeće prostora (bajtovi) grupisano po korisnicima",
            example = "{\"marko_pro\": 1048576, \"admin\": 2097152}")
    private Map<String, Long> byUser;

    @Schema(description = "Broj fajlova grupisan po bucketima",
            example = "{\"projekti\": 500, \"arhiva\": 750}")
    private Map<String, Long> byBucket;

}
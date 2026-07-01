package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detaljna statistika fajlova po specifičnoj kategoriji")
public class CategoryStatisticsResponse {

    @Schema(description = "Naziv kategorije fajlova", example = "IMAGE")
    private String category;

    @Schema(description = "Ukupan broj fajlova u ovoj kategoriji", example = "150")
    private long fileCount;

    @Schema(description = "Ukupna veličina svih fajlova u kategoriji (u bajtovima)", example = "204857600")
    private long totalSizeBytes;

    @Schema(description = "Ljudima čitljiva ukupna veličina fajlova u kategoriji", example = "200.0 MB")
    private String totalSizeFormatted;

    @Schema(description = "Prosječna veličina fajla u ovoj kategoriji (u bajtovima)", example = "1365717")
    private long averageSizeBytes;

    @Schema(description = "Ljudima čitljiva prosječna veličina fajla u kategoriji", example = "1.3 MB")
    private String averageSizeFormatted;

}
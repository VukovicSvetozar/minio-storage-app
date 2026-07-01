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
@Schema(description = "Podaci o zauzeću skladišnog prostora po pojedinačnom korisniku (za administrativni pregled)")
public class UserStorageStatisticsResponse {

    @Schema(description = "Korisničko ime korisnika", example = "marko_pro")
    private String username;

    @Schema(description = "Ukupan broj fajlova koje je ovaj korisnik otpremio", example = "240")
    private long fileCount;

    @Schema(description = "Ukupna veličina svih fajlova korisnika u bajtovima", example = "1073741824")
    private long totalSizeBytes;

    @Schema(description = "Ljudima čitljiv format ukupne veličine (npr. MB, GB)", example = "1.0 GB")
    private String totalSizeFormatted;

}
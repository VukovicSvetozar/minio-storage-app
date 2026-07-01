package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informacije o rezultatu otpremanja fajla")
public class FileUploadResponse {

    @Schema(description = "ID novokreiranog fajla u bazi", example = "101")
    private Long fileId;

    @Schema(description = "Originalni naziv fajla sa računara korisnika", example = "finansije_2026.pdf")
    private String originalFileName;

    @Schema(description = "UUID naziv objekta na storage serveru", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479.pdf")
    private String objectName;

    @Schema(description = "Naziv bucketa u koji je fajl sačuvan", example = "računi")
    private String bucketName;

    @Schema(description = "Veličina fajla u bajtovima", example = "1048576")
    private Long fileSize;

    @Schema(description = "Formatirana veličina fajla", example = "1.0 MB")
    private String formattedSize;

    @Schema(description = "MIME tip fajla", example = "application/pdf")
    private String contentType;

    @Schema(description = "Poruka o uspješnosti operacije", example = "Fajl je uspješno otpremljen.")
    private String message;

    @Schema(description = "Vrijeme kada je otpremanje završeno")
    private LocalDateTime uploadDate;

}
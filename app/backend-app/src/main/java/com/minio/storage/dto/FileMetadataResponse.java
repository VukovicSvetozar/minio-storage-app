package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detaljne informacije o fajlu i njegovim metapodacima")
public class FileMetadataResponse {

    @Schema(description = "Jedinstveni identifikator fajla u bazi podataka", example = "42")
    private Long id;

    @Schema(description = "Izvorni naziv fajla koji je korisnik otpremio", example = "slika.jpg")
    private String originalFileName;

    @Schema(description = "MIME tip sadržaja fajla", example = "image/jpeg")
    private String contentType;

    @Schema(description = "Veličina fajla izražena u bajtovima", example = "2048576")
    private Long fileSize;

    @Schema(description = "Ljudima čitljiva veličina fajla", example = "2.0 MB")
    private String formattedSize;

    @Schema(description = "Naziv skladišta (bucket) u kojem se fajl nalazi", example = "moje-slike")
    private String bucketName;

    @Schema(description = "Jedinstveni naziv objekta na serveru za skladištenje (UUID format)", example = "a3f7b2c1-8d4e-4a2b-9c3f-1e5d7a9b2c4f.jpg")
    private String objectName;

    @Schema(description = "Datum i vrijeme otpremanja fajla")
    private LocalDateTime uploadDate;

    @Schema(description = "Korisničko ime korisnika koji je otpremio fajl", example = "vuk_pro")
    private String uploadedBy;

    @Schema(description = "Opcioni opis ili bilješka o fajlu", example = "Slika sa odmora na moru")
    private String description;

    @Schema(description = "Tagovi povezani sa fajlom za lakšu pretragu", example = "[\"odmor\", \"more\", \"2026\"]")
    private String[] tags;

    @Schema(description = "Kategorija kojoj fajl pripada", example = "IMAGES")
    private String category;

    @Schema(description = "Indikator da li je fajl dostupan javno", example = "false")
    private boolean isPublic;

    @Schema(description = "Putanja do umanjenog prikaza (thumbnail) ako postoji", example = "/thumbnails/a3f7b2c1.jpg")
    private String thumbnailPath;

    @Schema(description = "Datum i vrijeme posljednje izmjene metapodataka")
    private LocalDateTime lastModified;

    @Schema(description = "Ekstenzija fajla (bez tačke)", example = "jpg")
    private String extension;

    @Schema(description = "Indikator da li je fajl slika", example = "true")
    private boolean isImage;

    @Schema(description = "Privremeni URL za direktno preuzimanje fajla (generiše se na zahtjev)")
    private String downloadUrl;

}
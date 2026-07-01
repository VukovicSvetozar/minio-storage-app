package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Izvještaj o nepoklapanjima (orphan objektima) između baze i storage servera (Dry-Run)")
public class OrphanCleanupReport {

    @Schema(description = "Naziv analiziranog bucketa", example = "moje-slike")
    private String bucketName;

    @Schema(description = "Fajlovi koji postoje na MinIO serveru, ali nemaju zapis u bazi podataka")
    private List<String> orphansInMinio;

    @Schema(description = "Zapisi u bazi podataka koji nemaju odgovarajući fizički fajl na MinIO serveru")
    private List<String> orphansInDatabase;

    @Schema(description = "Ukupan broj detektovanih nekonzistentnosti", example = "2")
    private int totalOrphans;

}
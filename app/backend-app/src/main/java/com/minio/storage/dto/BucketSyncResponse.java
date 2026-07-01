package com.minio.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zbirni izvještaj o ishodu sinhronizacije sa MinIO serverom")
public class BucketSyncResponse {

    @Schema(description = "Broj novootkrivenih bucketa uvezenih u lokalnu bazu", example = "2")
    private int imported;

    @Schema(description = "Broj postojećih bucketa čiji su podaci ažurirani", example = "5")
    private int updated;

    @Schema(description = "Broj bucketa preskočenih jer su označeni kao obrisani u bazi", example = "1")
    private int skipped;

    @Schema(description = "Broj bucketa koji nisu obrađeni zbog greške", example = "0")
    private int errors;

    @Schema(description = "Detaljna lista rezultata za svaki pojedinačni obrađeni bucket")
    private List<BucketSyncResult> results;

}
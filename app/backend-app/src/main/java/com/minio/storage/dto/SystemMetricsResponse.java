package com.minio.storage.dto;

import com.minio.storage.enums.HealthStatus;
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
@Schema(description = "Sveobuhvatan izvještaj o performansama i resursima sistema")
public class SystemMetricsResponse {

    @Schema(description = "Vrijeme prikupljanja metrika")
    private LocalDateTime timestamp;

    private SystemResources systemResources;
    private DatabaseMetrics databaseMetrics;
    private MinioMetrics minioMetrics;
    private ApplicationMetrics applicationMetrics;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Hardverski resursi servera (RAM, CPU, Disk)")
    public static class SystemResources {
        @Schema(description = "Ukupna RAM memorija (MB)", example = "16384")
        private Long totalMemoryMB;
        @Schema(description = "Zauzeta RAM memorija (MB)", example = "4096")
        private Long usedMemoryMB;
        @Schema(description = "Slobodna RAM memorija (MB)", example = "12288")
        private Long freeMemoryMB;
        @Schema(description = "Procenat zauzeća memorije", example = "25.0")
        private Double memoryUsagePercent;
        @Schema(description = "Broj dostupnih procesorskih jezgara", example = "8")
        private Integer availableProcessors;
        @Schema(description = "Prosječno opterećenje sistema (Load Average)", example = "0.75")
        private Double systemLoadAverage;
        @Schema(description = "Ukupan prostor na disku (GB)", example = "500")
        private Long totalDiskSpaceGB;
        @Schema(description = "Slobodan prostor na disku (GB)", example = "350")
        private Long freeDiskSpaceGB;
        @Schema(description = "Procenat zauzeća diska", example = "30.0")
        private Double diskUsagePercent;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Metrike baze podataka i konekcionog pool-a")
    public static class DatabaseMetrics {
        @Schema(description = "Status konekcije")
        private HealthStatus status;
        @Schema(description = "Broj aktivnih konekcija u pool-u", example = "3")
        private Integer activeConnections;
        @Schema(description = "Broj slobodnih konekcija u pool-u", example = "7")
        private Integer idleConnections;
        @Schema(description = "Maksimalan dozvoljen broj konekcija", example = "10")
        private Integer maxConnections;
        @Schema(description = "Ukupan broj registrovanih korisnika", example = "150")
        private Long totalUsers;
        @Schema(description = "Ukupan broj fajlova u bazi", example = "1200")
        private Long totalFiles;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Metrike MinIO sistema za skladištenje")
    public static class MinioMetrics {
        @Schema(description = "Status dostupnosti sistema za skladištenje")
        private HealthStatus status;
        @Schema(description = "Ukupna veličina svih fajlova u bajtovima", example = "1073741824")
        private Long usedStorageBytes;
        @Schema(description = "Ukupan broj bucket-a", example = "5")
        private Integer totalBuckets;
        @Schema(description = "Ukupan broj objekata na sistemu za skladištenje", example = "1200")
        private Long totalObjects;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @Schema(description = "Statistika korišćenja aplikacije")
    public static class ApplicationMetrics {
        @Schema(description = "Vrijeme rada aplikacije u sekundama", example = "86400")
        private Long uptimeSeconds;
        @Schema(description = "Verzija aplikacije", example = "1.0.0")
        private String version;
        @Schema(description = "Broj aktivnih korisnika", example = "45")
        private Long activeUsers;
        @Schema(description = "Broj logova aktivnosti u sistemu", example = "5000")
        private Long totalActivityLogs;
        @Schema(description = "Broj zahtjeva u posljednjih sat vremena", example = "320")
        private Long requestsLastHour;
        @Schema(description = "Ukupan broj aktivnih dijeljivih linkova u sistemu", example = "47")
        private Long totalShareLinks;
    }

}
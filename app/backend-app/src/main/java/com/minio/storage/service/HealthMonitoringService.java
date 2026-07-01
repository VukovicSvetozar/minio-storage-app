package com.minio.storage.service;

import com.minio.storage.dto.SystemHealthResponse;
import com.minio.storage.dto.SystemMetricsResponse;
import com.minio.storage.enums.HealthStatus;
import com.minio.storage.repository.ActivityLogRepository;
import com.minio.storage.repository.FileMetadataRepository;
import com.minio.storage.repository.ShareLinkRepository;
import com.minio.storage.repository.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.minio.ListBucketsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@RequiredArgsConstructor
public class HealthMonitoringService {

    private final DataSource dataSource;
    private final MinioClient minioClient;
    private final UserRepository userRepository;
    private final FileMetadataRepository fileRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final ActivityLogRepository activityLogRepository;
    private final long startTime = System.currentTimeMillis();

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    public SystemHealthResponse getSystemHealth() {
        log.debug("Provjera zdravlja sistema...");

        SystemHealthResponse.ComponentHealth dbHealth = checkDatabaseHealth();
        SystemHealthResponse.ComponentHealth minioHealth = checkMinioHealth();
        SystemHealthResponse.ComponentHealth appHealth = checkApplicationHealth();

        HealthStatus overallStatus = HealthStatus.UP;
        if (dbHealth.getStatus() == HealthStatus.DOWN ||
                minioHealth.getStatus() == HealthStatus.DOWN) {
            overallStatus = HealthStatus.DOWN;
        } else if (dbHealth.getStatus() == HealthStatus.DEGRADED ||
                minioHealth.getStatus() == HealthStatus.DEGRADED) {
            overallStatus = HealthStatus.DEGRADED;
        }

        return SystemHealthResponse.builder()
                .status(overallStatus)
                .timestamp(LocalDateTime.now())
                .version(applicationVersion)
                .uptimeSeconds((System.currentTimeMillis() - startTime) / 1000)
                .database(dbHealth)
                .minioStorage(minioHealth)
                .application(appHealth)
                .build();
    }

    public SystemMetricsResponse getSystemMetrics() {
        log.debug("Prikupljanje detaljnih sistemskih metrika...");

        return SystemMetricsResponse.builder()
                .timestamp(LocalDateTime.now())
                .systemResources(getSystemResources())
                .databaseMetrics(getDatabaseMetrics())
                .minioMetrics(getMinioMetrics())
                .applicationMetrics(getApplicationMetrics())
                .build();
    }

    private SystemHealthResponse.ComponentHealth checkDatabaseHealth() {
        long start = System.currentTimeMillis();
        Map<String, Object> details = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            details.put("vendor", connection.getMetaData().getDatabaseProductName());
            details.put("version", connection.getMetaData().getDatabaseProductVersion());
            boolean valid = connection.isValid(5);
            return SystemHealthResponse.ComponentHealth.builder()
                    .status(valid ? HealthStatus.UP : HealthStatus.DOWN)
                    .message(valid ? "Baza podataka je dostupna" : "Baza ne odgovara na validaciju")
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .details(details)
                    .build();
        } catch (Exception e) {
            log.error("Provjera baze podataka neuspješna", e);
            details.put("error", e.getMessage());
            return SystemHealthResponse.ComponentHealth.builder()
                    .status(HealthStatus.DOWN)
                    .message("Nije moguće uspostaviti konekciju sa bazom")
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .details(details)
                    .build();
        }
    }

    private SystemHealthResponse.ComponentHealth checkMinioHealth() {
        long start = System.currentTimeMillis();
        Map<String, Object> details = new HashMap<>();

        try {
            var buckets = minioClient.listBuckets(ListBucketsArgs.builder().build());
            long count = StreamSupport.stream(buckets.spliterator(), false).count();

            long responseTime = System.currentTimeMillis() - start;

            details.put("bucketsCount", count);
            details.put("accessible", true);

            return SystemHealthResponse.ComponentHealth.builder()
                    .status(HealthStatus.UP)
                    .message("MinIO storage je dostupan")
                    .responseTimeMs(responseTime)
                    .details(details)
                    .build();
        } catch (Exception e) {
            log.error("Provjera ispravnosti MinIO sistema nije uspjela", e);
            long responseTime = System.currentTimeMillis() - start;
            details.put("error", e.getMessage());
            details.put("accessible", false);

            return SystemHealthResponse.ComponentHealth.builder()
                    .status(HealthStatus.DOWN)
                    .message("Nije moguće pristupiti MinIO serveru")
                    .responseTimeMs(responseTime)
                    .details(details)
                    .build();
        }
    }

    private SystemHealthResponse.ComponentHealth checkApplicationHealth() {
        Map<String, Object> details = new HashMap<>();

        details.put("uptime", (System.currentTimeMillis() - startTime) / 1000 + "s");
        details.put("version", applicationVersion);
        details.put("javaVersion", System.getProperty("java.version"));

        return SystemHealthResponse.ComponentHealth.builder()
                .status(HealthStatus.UP)
                .message("Aplikacija radi ispravno")
                .responseTimeMs(0L)
                .details(details)
                .build();
    }

    private SystemMetricsResponse.SystemResources getSystemResources() {
        Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        double memoryPercent = (double) usedMemory / totalMemory * 100;

        double loadAvg = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();

        File root = new File("/");
        long totalDisk = root.getTotalSpace() / (1024 * 1024 * 1024);
        long freeDisk = root.getFreeSpace() / (1024 * 1024 * 1024);
        long usedDisk = totalDisk - freeDisk;
        double diskPercent = totalDisk > 0 ? (double) usedDisk / totalDisk * 100 : 0;

        return SystemMetricsResponse.SystemResources.builder()
                .totalMemoryMB(totalMemory)
                .usedMemoryMB(usedMemory)
                .freeMemoryMB(freeMemory)
                .memoryUsagePercent(round(memoryPercent))
                .availableProcessors(runtime.availableProcessors())
                .systemLoadAverage(loadAvg < 0 ? 0.0 : round(loadAvg))
                .totalDiskSpaceGB(totalDisk)
                .freeDiskSpaceGB(freeDisk)
                .diskUsagePercent(round(diskPercent))
                .build();
    }

    private SystemMetricsResponse.DatabaseMetrics getDatabaseMetrics() {
        try {
            int active = -1, idle = -1, max = -1;
            if (dataSource instanceof HikariDataSource ds) {
                HikariPoolMXBean pool = ds.getHikariPoolMXBean();
                if (pool != null) {
                    active = pool.getActiveConnections();
                    idle = pool.getIdleConnections();
                    max = ds.getMaximumPoolSize();
                }
            }

            return SystemMetricsResponse.DatabaseMetrics.builder()
                    .status(HealthStatus.UP)
                    .activeConnections(active)
                    .idleConnections(idle)
                    .maxConnections(max)
                    .totalUsers(userRepository.count())
                    .totalFiles(fileRepository.countAllActiveFiles())
                    .build();
        } catch (Exception e) {
            log.error("Greška pri čitanju metrika baze: {}", e.getMessage());
            return SystemMetricsResponse.DatabaseMetrics.builder().status(HealthStatus.DOWN).build();
        }
    }

    private SystemMetricsResponse.MinioMetrics getMinioMetrics() {
        try {
            var buckets = minioClient.listBuckets(ListBucketsArgs.builder().build());
            long totalBuckets = StreamSupport.stream(buckets.spliterator(), false).count();

            long totalFiles = fileRepository.countAllActiveFiles();
            long totalStorage = fileRepository.sumAllActiveFileSizes();

            return SystemMetricsResponse.MinioMetrics.builder()
                    .status(HealthStatus.UP)
                    .usedStorageBytes(totalStorage)
                    .totalBuckets((int) totalBuckets)
                    .totalObjects(totalFiles)
                    .build();
        } catch (Exception e) {
            log.error("Greška pri prikupljanju MinIO metrika", e);
            return SystemMetricsResponse.MinioMetrics.builder()
                    .status(HealthStatus.DOWN)
                    .build();
        }
    }

    private SystemMetricsResponse.ApplicationMetrics getApplicationMetrics() {
        long uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long activeUsers = userRepository.countByIsActive(true);
        long totalShareLinks = shareLinkRepository.count();
        long totalActivityLogs = activityLogRepository.count();

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long requestsLastHour = activityLogRepository.countByTimestampAfter(oneHourAgo);

        return SystemMetricsResponse.ApplicationMetrics.builder()
                .uptimeSeconds(uptimeSeconds)
                .version(applicationVersion)
                .activeUsers(activeUsers)
                .totalShareLinks(totalShareLinks)
                .totalActivityLogs(totalActivityLogs)
                .requestsLastHour(requestsLastHour)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

}
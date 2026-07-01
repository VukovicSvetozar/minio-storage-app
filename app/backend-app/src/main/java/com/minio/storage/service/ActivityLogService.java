package com.minio.storage.service;

import com.minio.storage.dto.ActivityLogFilterRequest;
import com.minio.storage.dto.ActivityLogResponse;
import com.minio.storage.entity.ActivityLog;
import com.minio.storage.enums.ActivityType;
import com.minio.storage.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public void logActivity(
            ActivityType type,
            String username,
            String description,
            String resourceType,
            Long resourceId,
            String resourceName,
            HttpServletRequest request) {

        ActivityLog activityLog = ActivityLog.success(
                type, username, description, resourceType, resourceId, resourceName
        );

        if (request != null) {
            activityLog.setIpAddress(getClientIp(request));
            activityLog.setUserAgent(request.getHeader("User-Agent"));
        }

        activityLogRepository.save(activityLog);
        log.info("Activity logged: {} by {} - {}", type, username, description);
    }

    @SuppressWarnings("unused")
    @Transactional
    public void logActivityWithDetails(
            ActivityType type,
            String username,
            String description,
            String resourceType,
            Long resourceId,
            String resourceName,
            String details,
            HttpServletRequest request) {

        ActivityLog activityLog = ActivityLog.success(
                type, username, description, resourceType, resourceId, resourceName
        );
        activityLog.setDetails(details);

        if (request != null) {
            activityLog.setIpAddress(getClientIp(request));
            activityLog.setUserAgent(request.getHeader("User-Agent"));
        }

        activityLogRepository.save(activityLog);
    }

    @Transactional
    public void logFailure(
            ActivityType type,
            String username,
            String description,
            String errorMessage,
            HttpServletRequest request) {

        ActivityLog activityLog = ActivityLog.failure(type, username, description, errorMessage);

        if (request != null) {
            activityLog.setIpAddress(getClientIp(request));
            activityLog.setUserAgent(request.getHeader("User-Agent"));
        }

        activityLogRepository.save(activityLog);
        log.warn("Activity failed: {} by {} - {}", type, username, errorMessage);
    }

    @SuppressWarnings("unused")
    public Page<ActivityLogResponse> getAllLogs(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ActivityLog> logs = activityLogRepository.findAll(pageable);

        return logs.map(this::convertToResponse);
    }

    @SuppressWarnings("unused")
    public Page<ActivityLogResponse> getLogsByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<ActivityLog> logs = activityLogRepository.findByUsername(username, pageable);

        return logs.map(this::convertToResponse);
    }

    @SuppressWarnings("unused")
    public List<ActivityLogResponse> getLogsByFileId(Long fileId) {
        List<ActivityLog> logs = activityLogRepository.findByResourceId(fileId);

        return logs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    public Page<ActivityLogResponse> filterLogs(ActivityLogFilterRequest request) {
        Sort.Direction direction = request.getSortDirection().equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(direction, request.getSortBy())
        );

        Page<ActivityLog> logs = activityLogRepository.findByFilters(
                request.getUsername(),
                request.getActivityType(),
                request.getResourceType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getSuccess(),
                pageable
        );

        return logs.map(this::convertToResponse);
    }

    @SuppressWarnings("unused")
    public List<ActivityLogResponse> getRecentLogs() {
        List<ActivityLog> logs = activityLogRepository.findTop10ByOrderByTimestampDesc();

        return logs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    public Map<String, Object> getActivityStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalLogs", activityLogRepository.count());

        stats.put("successfulActions", activityLogRepository.countBySuccess(true));
        stats.put("failedActions", activityLogRepository.countBySuccess(false));

        Map<String, Long> byType = new HashMap<>();
        for (ActivityType type : ActivityType.values()) {
            long count = activityLogRepository.countByActivityType(type);
            if (count > 0) {
                byType.put(type.name(), count);
            }
        }
        stats.put("logsByType", byType);

        return stats;
    }

    @SuppressWarnings("unused")
    public byte[] exportLogsToCSV(ActivityLogFilterRequest filter) {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ActivityLog> logs = activityLogRepository.findByFilters(
                filter.getUsername(), filter.getActivityType(), filter.getResourceType(),
                filter.getStartDate(), filter.getEndDate(), filter.getSuccess(), pageable
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
        } catch (Exception e) {
            log.error("Greška pri pisanju BOM-a", e);
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, java.nio.charset.StandardCharsets.UTF_8))) {

            writer.println("ID,Tip,Korisnik,Opis,Tip resursa,ID resursa,Naziv resursa,Vrijeme, Uspjeh, Greška,IP adresa");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (ActivityLog log : logs.getContent()) {
                writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        log.getId(),
                        log.getActivityType(),
                        log.getUsername(),
                        escapeCSV(log.getDescription()),
                        log.getResourceType() != null ? log.getResourceType() : "",
                        log.getResourceId() != null ? log.getResourceId() : "",
                        escapeCSV(log.getResourceName()),
                        log.getTimestamp().format(formatter),
                        log.isSuccess() ? "DA" : "NE",
                        escapeCSV(log.getErrorMessage()),
                        log.getIpAddress() != null ? log.getIpAddress() : ""
                );
            }
            writer.flush();
        } catch (Exception e) {
            log.error("Greška prilikom generisanja CSV fajla", e);
        }

        return baos.toByteArray();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private ActivityLogResponse convertToResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getActivityType(),
                log.getUsername(),
                log.getDescription(),
                log.getResourceType(),
                log.getResourceId(),
                log.getResourceName(),
                log.getDetails(),
                log.getIpAddress(),
                log.getTimestamp(),
                log.isSuccess(),
                log.getErrorMessage()
        );
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

}